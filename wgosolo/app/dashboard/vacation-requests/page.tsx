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
	PENDING: "bg-amber-50 text-amber-700 border-amber-200",
	APPROVED: "bg-green-50 text-green-700 border-green-200",
	REJECTED: "bg-red-50 text-red-700 border-red-200",
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
		api
			.get<VacationRequest[]>("/vacation-requests")
			.then(setRequests)
			.catch((e) => setError(e.message))
			.finally(() => setLoading(false));

		const load = () =>
			api.get<VacationRequest[]>("/vacation-requests").then(setRequests).catch(() => {});
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
			<div className="flex items-center justify-between pb-5 border-b">
				<div className="space-y-0.5">
					<h1 className="text-xl font-semibold tracking-tight">Pedidos de Férias</h1>
					<p className="text-sm text-muted-foreground">{filtered.length} pedidos</p>
				</div>
				<Link href="/dashboard/vacation-requests/new" className={cn(buttonVariants({ size: "sm" }))}>
					Novo pedido
				</Link>
			</div>

			<div className="flex gap-2">
				<Input
					placeholder="Pesquisar por colaborador..."
					value={search}
					onChange={(e) => handleFilterChange(setSearch)(e.target.value)}
					className="max-w-xs h-8 text-sm"
				/>
				<Select
					value={statusFilter || "ALL"}
					onValueChange={(v) => handleFilterChange(setStatusFilter)(v === "ALL" ? "" : (v ?? ""))}
				>
					<SelectTrigger className="min-w-36">
						<SelectValue />
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="ALL">Todos os estados</SelectItem>
						<SelectItem value="PENDING">Pendente</SelectItem>
						<SelectItem value="APPROVED">Aprovado</SelectItem>
						<SelectItem value="REJECTED">Rejeitado</SelectItem>
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
							<TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Colaborador</TableHead>
							<TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Início</TableHead>
							<TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Fim</TableHead>
							<TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Estado</TableHead>
							<TableHead className="font-medium text-xs uppercase tracking-wide text-muted-foreground">Revisto por</TableHead>
							<TableHead className="text-right" />
						</TableRow>
					</TableHeader>
					<TableBody>
						{loading && (
							<TableRow>
								<TableCell colSpan={6} className="py-12 text-center text-sm text-muted-foreground">
									A carregar...
								</TableCell>
							</TableRow>
						)}
						{!loading && pageItems.map((r) => (
							<TableRow key={r.id} className="hover:bg-slate-50/60">
								<TableCell className="font-medium text-sm">{r.userName}</TableCell>
								<TableCell className="text-sm text-muted-foreground">{fmtDate(r.startDate)}</TableCell>
								<TableCell className="text-sm text-muted-foreground">{fmtDate(r.endDate)}</TableCell>
								<TableCell>
									<span className={cn(
										"inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium",
										STATUS_CLASS[r.status],
									)}>
										{STATUS_LABEL[r.status]}
									</span>
								</TableCell>
								<TableCell className="text-sm text-muted-foreground">
									{r.reviewedByName ?? "—"}
								</TableCell>
								<TableCell className="text-right">
									<div className="flex items-center justify-end gap-1">
										{canReview && r.status === "PENDING" && (
											<>
												<Button
													size="sm"
													className="h-7 text-xs"
													onClick={() => handleApprove(r.id)}
												>
													Aprovar
												</Button>
												<Button
													size="sm"
													variant="ghost"
													className="h-7 text-xs text-destructive hover:text-destructive hover:bg-destructive/10"
													onClick={() => handleReject(r.id)}
												>
													Rejeitar
												</Button>
											</>
										)}
										{r.status === "PENDING" && r.userId === currentUserId && (
											<Link
												href={`/dashboard/vacation-requests/${r.id}`}
												className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "h-7 text-xs")}
											>
												Editar
											</Link>
										)}
										{r.status === "PENDING" && (
											<Button
												size="sm"
												variant="ghost"
												className="h-7 text-xs text-muted-foreground hover:text-foreground"
												onClick={() => handleCancel(r.id)}
											>
												Cancelar
											</Button>
										)}
									</div>
								</TableCell>
							</TableRow>
						))}
						{!loading && filtered.length === 0 && (
							<TableRow>
								<TableCell colSpan={6} className="py-12 text-center text-sm text-muted-foreground">
									Nenhum pedido encontrado.
								</TableCell>
							</TableRow>
						)}
					</TableBody>
				</Table>
			</div>

			{!loading && totalPages > 1 && (
				<div className="flex items-center justify-between">
					<span className="text-xs text-muted-foreground">
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
