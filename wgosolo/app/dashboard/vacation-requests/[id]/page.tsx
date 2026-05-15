"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";
import { api } from "../../../lib/api";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

type VacationFields = {
	startDate: string;
	endDate: string;
};

type VacationRequest = {
	id: number;
	startDate: string;
	endDate: string;
	status: string;
};

export default function EditVacationRequestPage() {
	const router = useRouter();
	const params = useParams<{ id: string }>();
	const [loading, setLoading] = useState(true);

	const {
		register,
		handleSubmit,
		reset,
		watch,
		formState: { errors, isSubmitting },
	} = useForm<VacationFields>();

	const startDate = watch("startDate");

	useEffect(() => {
		api
			.get<VacationRequest>(`/vacation-requests/${params.id}`)
			.then((data) => {
				if (data.status !== "PENDING") {
					toast.error("Só é possível editar pedidos pendentes");
					router.replace("/dashboard/vacation-requests");
					return;
				}
				reset({ startDate: data.startDate, endDate: data.endDate });
			})
			.catch(() => {
				toast.error("Pedido não encontrado ou sem acesso");
				router.replace("/dashboard/vacation-requests");
			})
			.finally(() => setLoading(false));
	}, [params.id, reset, router]);

	async function onSubmit(data: VacationFields) {
		try {
			await api.put(`/vacation-requests/${params.id}`, {
				startDate: data.startDate,
				endDate: data.endDate,
			});
			toast.success("Pedido actualizado");
			router.push("/dashboard/vacation-requests");
		} catch (e: unknown) {
			toast.error((e as Error).message);
		}
	}

	if (loading) {
		return <div className="text-sm text-muted-foreground">A carregar...</div>;
	}

	return (
		<div className="max-w-lg space-y-3">
			<Link
				href="/dashboard/vacation-requests"
				className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "-ml-2")}
			>
				<ChevronLeft className="size-4" />
				Pedidos de Férias
			</Link>

			<Card>
				<CardHeader className="border-b">
					<CardTitle className="text-base">Editar pedido de férias</CardTitle>
				</CardHeader>
				<CardContent className="pt-6">
					<form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
						<div className="space-y-1.5">
							<Label htmlFor="startDate">Data de início</Label>
							<Input
								id="startDate"
								type="date"
								aria-invalid={!!errors.startDate}
								{...register("startDate", { required: "Data de início obrigatória" })}
							/>
							{errors.startDate && (
								<p className="text-xs text-destructive">{errors.startDate.message}</p>
							)}
						</div>

						<div className="space-y-1.5">
							<Label htmlFor="endDate">Data de fim</Label>
							<Input
								id="endDate"
								type="date"
								min={startDate}
								aria-invalid={!!errors.endDate}
								{...register("endDate", {
									required: "Data de fim obrigatória",
									validate: (v) =>
										!startDate ||
										v >= startDate ||
										"Data de fim deve ser igual ou posterior ao início",
								})}
							/>
							{errors.endDate && (
								<p className="text-xs text-destructive">{errors.endDate.message}</p>
							)}
						</div>

						<div className="flex gap-3 pt-1">
							<Button type="submit" disabled={isSubmitting}>
								{isSubmitting ? "A guardar..." : "Guardar"}
							</Button>
							<Button
								type="button"
								variant="outline"
								onClick={() => router.push("/dashboard/vacation-requests")}
							>
								Cancelar
							</Button>
						</div>
					</form>
				</CardContent>
			</Card>
		</div>
	);
}
