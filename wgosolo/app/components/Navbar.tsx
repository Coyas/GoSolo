"use client";

import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { clearSession, getRole, getUserName } from "../lib/auth";
import { buttonVariants } from "@/components/ui/button";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { cn } from "@/lib/utils";

export default function Navbar() {
  const router = useRouter();
  const pathname = usePathname();
  const role = getRole();
  const name = getUserName();

  function logout() {
    clearSession();
    router.replace("/login");
  }

  const links = [
    { href: "/dashboard/vacation-requests", label: "Férias" },
    ...(role === "ADMIN"
      ? [{ href: "/dashboard/users", label: "Colaboradores" }]
      : []),
  ];

  return (
    <header className="border-b bg-background">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-6 py-3">
        <span className="text-base font-semibold">GoSolo</span>
        <Separator orientation="vertical" className="h-5" />
        <nav className="flex gap-1 flex-1">
          {links.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              className={cn(
                buttonVariants({
                  variant: pathname.startsWith(l.href) ? "secondary" : "ghost",
                  size: "sm",
                })
              )}
            >
              {l.label}
            </Link>
          ))}
        </nav>
        <div className="flex items-center gap-3">
          {name && <span className="text-sm font-medium">{name}</span>}
          <span className="inline-flex items-center rounded-full border border-blue-200 bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700">
            {role}
          </span>
          <Button variant="outline" size="sm" onClick={logout}>
            Sair
          </Button>
        </div>
      </div>
    </header>
  );
}
