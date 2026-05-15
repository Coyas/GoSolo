"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { api } from "../../lib/api";
import { getRole, getUserId } from "../../lib/auth";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

type VacationRequest = {
  id: number;
  userId: number;
  userName: string;
  startDate: string;
  endDate: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  reviewedByName: string | null;
};

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Pendente",
  APPROVED: "Aprovado",
  REJECTED: "Rejeitado",
};

const STATUS_CLASS: Record<string, string> = {
  PENDING: "bg-amber-100 text-amber-700 border-amber-200",
  APPROVED: "bg-green-100 text-green-700 border-green-200",
  REJECTED: "bg-red-100 text-red-700 border-red-200",
};

function fmtDate(iso: string) {
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}

const PAGE_SIZE = 8;

export default function VacationRequestsPage() {
  const [requests, setRequests] = useState<VacationRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [page, setPage] = useState(1);
  const role = getRole();
  const currentUserId = getUserId();
  const canReview = role === "ADMIN" || role === "MANAGER";

  useEffect(() => {
    const load = () =>
      api.get<VacationRequest[]>("/vacation-requests").then(setRequests).catch(() => {});

    api
      .get<VacationRequest[]>("/vacation-requests")
      .then(setRequests)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));

    const id = setInterval(load, 15_000);
    return () => clearInterval(id);
  }, []);

  const filtered = useMemo(() => {
    const s = search.toLowerCase();
    return requests.filter((r) => {
      const matchesSearch = !s || r.userName.toLowerCase().includes(s);
      const matchesStatus = !statusFilter || r.status === statusFilter;
      return matchesSearch && matchesStatus;
    });
  }, [requests, search, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pageItems = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  function handleFilterChange(setter: (v: string) => void) {
    return (v: string) => { setter(v); setPage(1); };
  }

  async function handleApprove(id: number) {
    try {
      const updated = await api.patch<VacationRequest>(`/vacation-requests/${id}/approve`);
      setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
      toast.success("Pedido aprovado");
    } catch (e: unknown) {
      toast.error((e as Error).message);
    }
  }

  async function handleReject(id: number) {
    try {
      const updated = await api.patch<VacationRequest>(`/vacation-requests/${id}/reject`);
      setRequests((prev) => prev.map((r) => (r.id === id ? updated : r)));
      toast.success("Pedido rejeitado");
    } catch (e: unknown) {
      toast.error((e as Error).message);
    }
  }

  async function handleCancel(id: number) {
    if (!confirm("Cancelar este pedido?")) return;
    try {
      await api.delete(`/vacation-requests/${id}`);
      setRequests((prev) => prev.filter((r) => r.id !== id));
      toast.success("Pedido cancelado");
    } catch (e: unknown) {
      toast.error((e as Error).message);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-semibold">Pedidos de Férias</h1>
          <p className="text-sm text-muted-foreground mt-0.5">{filtered.length} pedidos</p>
        </div>
        <Link href="/dashboard/vacation-requests/new" className={cn(buttonVariants())}>
          Novo pedido
        </Link>
      </div>

      <div className="flex gap-3">
        <Input
          placeholder="Pesquisar por colaborador..."
          value={search}
          onChange={(e) => handleFilterChange(setSearch)(e.target.value)}
          className="max-w-xs"
        />
        <select
          value={statusFilter}
          onChange={(e) => handleFilterChange(setStatusFilter)(e.target.value)}
          className="h-8 rounded-lg border border-input bg-transparent px-2.5 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          <option value="">Todos os estados</option>
          <option value="PENDING">Pendente</option>
          <option value="APPROVED">Aprovado</option>
          <option value="REJECTED">Rejeitado</option>
        </select>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <div className="rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Colaborador</TableHead>
              <TableHead>Início</TableHead>
              <TableHead>Fim</TableHead>
              <TableHead>Estado</TableHead>
              <TableHead>Revisto por</TableHead>
              <TableHead className="text-right">Acções</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={6} className="py-8 text-center text-muted-foreground">
                  A carregar...
                </TableCell>
              </TableRow>
            )}
            {!loading && pageItems.map((r) => (
              <TableRow key={r.id}>
                <TableCell className="font-medium">{r.userName}</TableCell>
                <TableCell>{fmtDate(r.startDate)}</TableCell>
                <TableCell>{fmtDate(r.endDate)}</TableCell>
                <TableCell>
                  <span className={cn(
                    "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium",
                    STATUS_CLASS[r.status]
                  )}>
                    {STATUS_LABEL[r.status]}
                  </span>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {r.reviewedByName ?? "—"}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex items-center justify-end gap-2">
                    {canReview && r.status === "PENDING" && (
                      <>
                        <Button size="sm" onClick={() => handleApprove(r.id)}>Aprovar</Button>
                        <Button size="sm" variant="destructive" onClick={() => handleReject(r.id)}>Rejeitar</Button>
                      </>
                    )}
                    {r.status === "PENDING" && r.userId === currentUserId && (
                      <Link
                        href={`/dashboard/vacation-requests/${r.id}`}
                        className={cn(buttonVariants({ variant: "ghost", size: "sm" }))}
                      >
                        Editar
                      </Link>
                    )}
                    {r.status === "PENDING" && (
                      <Button size="sm" variant="outline" onClick={() => handleCancel(r.id)}>
                        Cancelar
                      </Button>
                    )}
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {!loading && filtered.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-8 text-center text-muted-foreground">
                  Nenhum pedido encontrado.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            Página {currentPage} de {totalPages}
          </span>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === 1}
              onClick={() => setPage((p) => p - 1)}
            >
              Anterior
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={currentPage === totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Seguinte
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
