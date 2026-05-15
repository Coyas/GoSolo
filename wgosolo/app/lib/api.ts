import { getToken, clearSession } from "./auth";

/*
 * Cliente HTTP centralizado — todos os pedidos ao backend passam por aqui.
 * Injeta o token JWT automaticamente e trata erros de forma consistente:
 *   401 → limpa sessão e redireciona para login
 *   outros erros → lança Error com a mensagem do backend
 *   204 → devolve undefined (sem body para parsear)
 */

const BASE = process.env.NEXT_PUBLIC_API_URL + "/api/v1";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
	const token = getToken();
	const headers: Record<string, string> = {
		"Content-Type": "application/json",
		...(options.headers as Record<string, string>),
	};
	if (token) headers["Authorization"] = `Bearer ${token}`;

	const res = await fetch(BASE + path, { ...options, headers });

	if (res.status === 401) {
		// sessão expirada ou inválida — ka pode continuar autenticado
		clearSession();
		window.location.href = "/login";
		throw new Error("Unauthorized");
	}

	if (!res.ok) {
		const body = await res.json().catch(() => ({ message: "Request failed" }));
		throw new Error(body.message ?? "Request failed");
	}

	if (res.status === 204) return undefined as T;
	return res.json();
}

export const api = {
	get: <T>(path: string) => request<T>(path),
	post: <T>(path: string, body: unknown) =>
		request<T>(path, { method: "POST", body: JSON.stringify(body) }),
	put: <T>(path: string, body: unknown) =>
		request<T>(path, { method: "PUT", body: JSON.stringify(body) }),
	patch: <T>(path: string) => request<T>(path, { method: "PATCH" }),
	delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
