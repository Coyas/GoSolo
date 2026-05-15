"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { getSession } from "../lib/auth";
import Navbar from "../components/Navbar";

export default function DashboardLayout({
	children,
}: {
	children: React.ReactNode;
}) {
	const router = useRouter();
	const [ready, setReady] = useState(false);

	useEffect(() => {
		if (!getSession()) {
			router.replace("/login");
		} else {
			setReady(true);
		}
	}, [router]);

	if (!ready) return null;

	return (
		<div className="min-h-screen bg-slate-50/60">
			<Navbar />
			<main className="mx-auto max-w-6xl px-6 py-10">{children}</main>
		</div>
	);
}
