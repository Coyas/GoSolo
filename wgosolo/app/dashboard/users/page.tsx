"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { api } from "../../lib/api";
import { getRole } from "../../lib/auth";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

type User = {
  id: number;
  name: string;
  email: string;
  role: string;
  managerName: string | null;
};

const PAGE_SIZE = 8;

const ROLE_LABEL: Record<string, string> = {
  ADMIN: "Admin",
  MANAGER: "Manager",
  COLLABORATOR: "Collaborator",
};

export default function UsersPage() {
  const router = useRouter();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [page, setPage] = useState(1);

  useEffect(() => {
    if (getRole() !== "ADMIN") {
      router.replace("/dashboard");
      return;
    }

    api
      .get<User[]>("/users")
      .then(setUsers)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));

    const load = () => api.get<User[]>("/users").then(setUsers).catch(() => {});
    const id = setInterval(load, 15_000);
    return () => clearInterval(id);
  }, [router]);

  const filtered = useMemo(() => {
    const s = search.toLowerCase();
    return users.filter((u) => {
      const matchesSearch = !s || u.name.toLowerCase().includes(s) || u.email.toLowerCase().includes(s);
      const matchesRole = !roleFilter || u.role === roleFilter;
      return matchesSearch && matchesRole;
    });
  }, [users, search, roleFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pageItems = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  function handleFilterChange(setter: (v: string) => void) {
    return (v: string) => { setter(v); setPage(1); };
  }

  async function handleDelete(id: number) {
    if (!confirm("Remover este colaborador?")) return;
    try {
      await api.delete(`/users/${id}`);
      setUsers((prev) => prev.filter((u) => u.id !== id));
      toast.success("Colaborador removido");
    } catch (e: unknown) {
      toast.error((e as Error).message);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between pb-5 border-b">
        <div className="space-y-0.5">
          <h1 className="text-xl font-semibold tracking-tight">Colaboradores</h1>
          <p className="text-sm text-muted-foreground">{filtered.length} utilizadores</p>
        </div>
        <Link href="/dashboard/users/new" className={cn(buttonVariants({ size: "sm" }))}>
          Novo colaborador
        </Link>
      </div>

      <div className="flex gap-2">
        <Input
          placeholder="Pesquisar por nome ou email..."
          value={search}
          onChange={(e) => handleFilterChange(setSearch)(e.target.value)}
          className="max-w-xs h-8 text-sm"
        />
        <Select
          value={roleFilter || "ALL"}
          onValueChange={(v) => handleFilterChange(setRoleFilter)(v === "ALL" ? "" : (v ?? ""))}
        >
          <SelectTrigger className="min-w-36">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Todos os roles</SelectItem>
            <SelectItem value="ADMIN">Admin</SelectItem>
            <SelectItem value="MANAGER">Manager</SelectItem>
            <SelectItem value="COLLABORATOR">Collaborator</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <div className="rounded-lg border overflow-hidden bg-white">
        <Table>
          <TableHeader>
            <TableRow className="bg-slate-50 hover:bg-slate-50 border-b">
              <TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Nome</TableHead>
              <TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Email</TableHead>
              <TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Role</TableHead>
              <TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Manager</TableHead>
              <TableHead className="text-right" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={5} className="py-12 text-center text-sm text-muted-foreground">
                  A carregar...
                </TableCell>
              </TableRow>
            )}
            {!loading && pageItems.map((u) => (
              <TableRow key={u.id} className="hover:bg-slate-50/60">
                <TableCell className="font-medium text-sm">{u.name}</TableCell>
                <TableCell className="text-sm text-muted-foreground">{u.email}</TableCell>
                <TableCell>
                  <span className="inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium text-foreground bg-muted/50">
                    {ROLE_LABEL[u.role] ?? u.role}
                  </span>
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">{u.managerName ?? "—"}</TableCell>
                <TableCell className="text-right">
                  <div className="flex items-center justify-end gap-1">
                    <Link
                      href={`/dashboard/users/${u.id}`}
                      className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "h-7 text-xs")}
                    >
                      Editar
                    </Link>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="h-7 text-xs text-destructive hover:text-destructive hover:bg-destructive/10"
                      onClick={() => handleDelete(u.id)}
                    >
                      Remover
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {!loading && filtered.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="py-12 text-center text-sm text-muted-foreground">
                  Nenhum colaborador encontrado.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>

      {!loading && totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground text-xs">
            Página {currentPage} de {totalPages}
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" disabled={currentPage === 1} onClick={() => setPage((p) => p - 1)}>
              Anterior
            </Button>
            <Button variant="outline" size="sm" disabled={currentPage === totalPages} onClick={() => setPage((p) => p + 1)}>
              Seguinte
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
