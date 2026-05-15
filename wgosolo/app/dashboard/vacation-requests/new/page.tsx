"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { cn } from "@/lib/utils";
import { api } from "../../../lib/api";
import { Button, buttonVariants } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

type VacationFields = {
	startDate: string;
	endDate: string;
};

export default function NewVacationRequestPage() {
	const router = useRouter();
	const [serverError, setServerError] = useState("");

	const {
		register,
		handleSubmit,
		watch,
		formState: { errors, isSubmitting },
	} = useForm<VacationFields>();

	const startDate = watch("startDate");

	async function onSubmit(data: VacationFields) {
		setServerError("");
		try {
			await api.post("/vacation-requests", {
				userId: 0,
				startDate: data.startDate,
				endDate: data.endDate,
			});
			router.push("/dashboard/vacation-requests");
		} catch (e: unknown) {
			setServerError((e as Error).message);
		}
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
					<CardTitle className="text-base">Novo pedido de férias</CardTitle>
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

						{serverError && (
							<Alert variant="destructive">
								<AlertDescription>{serverError}</AlertDescription>
							</Alert>
						)}

						<div className="flex gap-3 pt-1">
							<Button type="submit" disabled={isSubmitting}>
								{isSubmitting ? "A submeter..." : "Submeter pedido"}
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
