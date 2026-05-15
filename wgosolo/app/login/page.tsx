"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { useRouter } from "next/navigation";
import { saveSession } from "../lib/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

type LoginFields = {
  email: string;
  password: string;
};

export default function LoginPage() {
  const router = useRouter();
  const [serverError, setServerError] = useState("");

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFields>();

  async function onSubmit(data: LoginFields) {
    setServerError("");
    try {
      const res = await fetch(
        process.env.NEXT_PUBLIC_API_URL + "/api/v1/auth/login",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(data),
        }
      );

      if (!res.ok) {
        setServerError("Credenciais inválidas.");
        return;
      }

      const body = await res.json();
      saveSession({ token: body.token, role: body.role, userId: body.userId, name: body.name });
      router.replace("/dashboard");
    } catch {
      setServerError("Erro de ligação ao servidor.");
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Painel esquerdo — branding */}
      <div className="hidden lg:flex lg:w-5/12 bg-slate-900 flex-col justify-between p-12 select-none">
        <span className="text-white text-lg font-semibold tracking-tight">GoSolo</span>
        <div className="space-y-4">
          <p className="text-white text-3xl font-semibold tracking-tight leading-snug">
            Gestão de Férias<br />simples e directa.
          </p>
          <p className="text-slate-400 text-sm leading-relaxed max-w-xs">
            Plataforma interna da TaskFlow para gestão de colaboradores e pedidos de férias.
          </p>
        </div>
        <p className="text-slate-600 text-xs">© 2026 TaskFlow Ltda.</p>
      </div>

      {/* Painel direito — formulário */}
      <div className="flex-1 flex items-center justify-center px-8">
        <div className="w-full max-w-sm space-y-8">
          <div className="space-y-1.5">
            <h1 className="text-2xl font-semibold tracking-tight">Bem-vindo</h1>
            <p className="text-sm text-muted-foreground">
              Introduz as tuas credenciais para continuar.
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="email@empresa.pt"
                aria-invalid={!!errors.email}
                {...register("email", { required: "Email obrigatório" })}
              />
              {errors.email && (
                <p className="text-xs text-destructive">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                aria-invalid={!!errors.password}
                {...register("password", { required: "Password obrigatória" })}
              />
              {errors.password && (
                <p className="text-xs text-destructive">{errors.password.message}</p>
              )}
            </div>

            {serverError && (
              <p className="text-sm text-destructive">{serverError}</p>
            )}

            <Button type="submit" disabled={isSubmitting} className="w-full">
              {isSubmitting ? "A entrar..." : "Entrar"}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
