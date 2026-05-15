"use client";

import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { clearSession, getRole, getUserName } from "../lib/auth";
import { Button } from "@/components/ui/button";
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
    <header className="sticky top-0 z-40 border-b border-border bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/80">
      <div className="mx-auto flex max-w-6xl items-center h-14 px-6 gap-6">
        <span className="text-sm font-semibold tracking-tight shrink-0">GoSolo</span>

        <nav className="flex items-center gap-1 flex-1">
          {links.map((l) => {
            const active = pathname.startsWith(l.href);
            return (
              <Link
                key={l.href}
                href={l.href}
                className={cn(
                  "px-3 py-1.5 text-sm rounded-md transition-colors",
                  active
                    ? "text-foreground font-medium bg-muted"
                    : "text-muted-foreground hover:text-foreground hover:bg-muted/60"
                )}
              >
                {l.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-4 shrink-0">
          {name && (
            <div className="text-right leading-none">
              <p className="text-sm font-medium">{name}</p>
              <p className="text-xs text-muted-foreground mt-0.5">
                {role ? role.charAt(0) + role.slice(1).toLowerCase() : ""}
              </p>
            </div>
          )}
          <Button variant="outline" size="sm" onClick={logout}>
            Sair
          </Button>
        </div>
      </div>
    </header>
  );
}
