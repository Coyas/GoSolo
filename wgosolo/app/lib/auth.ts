export type AuthSession = {
	token: string;
	role: "ADMIN" | "MANAGER" | "COLLABORATOR";
	userId: number;
	name: string;
};

const KEY = "gosolo_session";

/*
 * Gestão de sessão no localStorage — guarda o token JWT e os dados do utilizador.
 * A sessão é validada em cada acesso: se o token estiver expirado é removida
 * automaticamente, sem precisar de chamada ao backend.
 */

// decode do payload JWT (base64) para ler o campo exp — não valida assinatura
function isTokenExpired(token: string): boolean {
	try {
		const payload = JSON.parse(atob(token.split(".")[1]));
		return payload.exp * 1000 < Date.now();
	} catch {
		return true;
	}
}

export function getSession(): AuthSession | null {
	if (typeof window === "undefined") return null; // SSR guard
	const raw = localStorage.getItem(KEY);
	if (!raw) return null;
	try {
		const session = JSON.parse(raw) as AuthSession;
		if (isTokenExpired(session.token)) {
			localStorage.removeItem(KEY);
			return null;
		}
		return session;
	} catch {
		return null;
	}
}

export function saveSession(session: AuthSession): void {
	localStorage.setItem(KEY, JSON.stringify(session));
}

export function clearSession(): void {
	localStorage.removeItem(KEY);
}

export function getToken(): string | null {
	return getSession()?.token ?? null;
}

export function getRole(): AuthSession["role"] | null {
	return getSession()?.role ?? null;
}

export function getUserId(): number | null {
	return getSession()?.userId ?? null;
}

export function getUserName(): string | null {
	return getSession()?.name ?? null;
}
