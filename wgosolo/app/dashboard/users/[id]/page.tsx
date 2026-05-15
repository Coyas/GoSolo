"use client";

import { use } from "react";
import Link from "next/link";
import { ChevronLeft } from "lucide-react";
import { cn } from "@/lib/utils";
import { buttonVariants } from "@/components/ui/button";
import UserForm from "../../../components/UserForm";

export default function EditUserPage({
	params,
}: {
	params: Promise<{ id: string }>;
}) {
	const { id } = use(params);

	return (
		<div className="max-w-lg space-y-3">
			<Link
				href="/dashboard/users"
				className={cn(buttonVariants({ variant: "ghost", size: "sm" }), "-ml-2")}
			>
				<ChevronLeft className="size-4" />
				Colaboradores
			</Link>
			<UserForm title="Editar colaborador" userId={Number(id)} />
		</div>
	);
}
