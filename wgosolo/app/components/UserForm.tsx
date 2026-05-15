"use client";

import { useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import { useRouter } from "next/navigation";
import { api } from "../lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type UserData = {
  id: number;
  name: string;
  email: string;
  role: string;
  managerId: number | null;
};

type UserFields = {
  name: string;
  email: string;
  password: string;
  role: string;
  managerId: string;
};

type Props = {
  userId?: number;
};

const ROLES = ["ADMIN", "MANAGER", "COLLABORATOR"];

export default function UserForm({ userId }: Props) {
  const router = useRouter();
  const isEdit = Boolean(userId);
  const [managers, setManagers] = useState<UserData[]>([]);
  const [serverError, setServerError] = useState("");

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<UserFields>({
    defaultValues: { name: "", email: "", password: "", role: "COLLABORATOR", managerId: "" },
  });

  useEffect(() => {
    api.get<UserData[]>("/users").then((users) =>
      setManagers(users.filter((u) => u.role === "MANAGER" || u.role === "ADMIN"))
    );

    if (userId) {
      api.get<UserData>(`/users/${userId}`).then((u) =>
        reset({
          name: u.name,
          email: u.email,
          password: "",
          role: u.role,
          managerId: u.managerId ? String(u.managerId) : "",
        })
      );
    }
  }, [userId, reset]);

  async function onSubmit(data: UserFields) {
    setServerError("");
    const body = {
      name: data.name,
      email: data.email,
      role: data.role,
      managerId: data.managerId ? Number(data.managerId) : null,
      ...(data.password ? { password: data.password } : {}),
    };

    try {
      if (isEdit) {
        await api.put(`/users/${userId}`, body);
      } else {
        await api.post("/users", body);
      }
      router.push("/dashboard/users");
    } catch (e: unknown) {
      setServerError((e as Error).message);
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="max-w-lg space-y-5">
      <div className="space-y-1.5">
        <Label htmlFor="name">Nome</Label>
        <Input
          id="name"
          aria-invalid={!!errors.name}
          {...register("name", { required: "Nome obrigatório" })}
        />
        {errors.name && <p className="text-xs text-destructive">{errors.name.message}</p>}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="email">Email</Label>
        <Input
          id="email"
          type="email"
          aria-invalid={!!errors.email}
          {...register("email", {
            required: "Email obrigatório",
            pattern: { value: /\S+@\S+\.\S+/, message: "Email inválido" },
          })}
        />
        {errors.email && <p className="text-xs text-destructive">{errors.email.message}</p>}
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="password">
          Password{" "}
          {isEdit && (
            <span className="text-muted-foreground font-normal">(deixar vazio para manter)</span>
          )}
        </Label>
        <Input
          id="password"
          type="password"
          aria-invalid={!!errors.password}
          {...register("password", {
            validate: (v) =>
              isEdit || v.length >= 6 || "Password deve ter pelo menos 6 caracteres",
          })}
        />
        {errors.password && (
          <p className="text-xs text-destructive">{errors.password.message}</p>
        )}
      </div>

      <div className="space-y-1.5">
        <Label>Role</Label>
        <Controller
          name="role"
          control={control}
          rules={{ required: true }}
          render={({ field }) => (
            <Select value={field.value} onValueChange={(v) => field.onChange(v ?? field.value)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {ROLES.map((r) => (
                  <SelectItem key={r} value={r}>{r}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
      </div>

      <div className="space-y-1.5">
        <Label>Manager</Label>
        <Controller
          name="managerId"
          control={control}
          render={({ field }) => (
            <Select
              value={field.value}
              onValueChange={(v) => field.onChange(!v || v === "none" ? "" : v)}
            >
              <SelectTrigger>
                <SelectValue placeholder="— sem manager —" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">— sem manager —</SelectItem>
                {managers.map((m) => (
                  <SelectItem key={m.id} value={String(m.id)}>{m.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        />
      </div>

      {serverError && (
        <Alert variant="destructive">
          <AlertDescription>{serverError}</AlertDescription>
        </Alert>
      )}

      <div className="flex gap-3">
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "A guardar..." : isEdit ? "Guardar alterações" : "Criar colaborador"}
        </Button>
        <Button type="button" variant="outline" onClick={() => router.push("/dashboard/users")}>
          Cancelar
        </Button>
      </div>
    </form>
  );
}
