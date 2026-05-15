"use client";

import { use } from "react";
import UserForm from "../../../components/UserForm";

export default function EditUserPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);

  return (
    <div>
      <h1 className="text-xl font-semibold text-zinc-900 mb-6">Editar colaborador</h1>
      <UserForm userId={Number(id)} />
    </div>
  );
}
