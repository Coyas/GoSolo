import {
	getSession,
	saveSession,
	clearSession,
	getToken,
	getUserId,
	getUserName,
	type AuthSession,
} from "../../app/lib/auth";

/*
 * Testes unitários do módulo auth — gestão de sessão no localStorage.
 * O jsdom do Jest simula window e localStorage, por isso correm sem browser.
 */

const KEY = "gosolo_session";

// gera um JWT mínimo com o exp que queremos — ka precisa de ser assinado pa testar
function makeToken(expOffsetSeconds: number): string {
	const payload = {
		sub: "test@test.com",
		exp: Math.floor(Date.now() / 1000) + expOffsetSeconds,
	};
	return `header.${btoa(JSON.stringify(payload))}.signature`;
}

function makeSession(token: string): AuthSession {
	return { token, role: "COLLABORATOR", userId: 1, name: "Ana" };
}

beforeEach(() => localStorage.clear());

describe("saveSession / getSession", () => {
	it("getSession returns null when localStorage is empty", () => {
		expect(getSession()).toBeNull();
	});

	it("getSession returns session when token is valid", () => {
		const session = makeSession(makeToken(3600));
		saveSession(session);
		const result = getSession();
		expect(result?.userId).toBe(1);
		expect(result?.name).toBe("Ana");
	});

	it("getSession returns null and removes key when token is expired", () => {
		// token expirou há 1 hora — deve ser apagado automaticamente
		const session = makeSession(makeToken(-3600));
		saveSession(session);
		expect(getSession()).toBeNull();
		expect(localStorage.getItem(KEY)).toBeNull();
	});

	it("getSession returns null when stored value is malformed JSON", () => {
		localStorage.setItem(KEY, "isto_nao_e_json");
		expect(getSession()).toBeNull();
	});
});

describe("clearSession", () => {
	it("removes the session from localStorage", () => {
		saveSession(makeSession(makeToken(3600)));
		clearSession();
		expect(localStorage.getItem(KEY)).toBeNull();
	});
});

describe("getToken", () => {
	it("returns token when session is valid", () => {
		const token = makeToken(3600);
		saveSession(makeSession(token));
		expect(getToken()).toBe(token);
	});

	it("returns null when no session exists", () => {
		expect(getToken()).toBeNull();
	});
});

describe("getUserId / getUserName", () => {
	it("returns userId and name from active session", () => {
		saveSession(makeSession(makeToken(3600)));
		expect(getUserId()).toBe(1);
		expect(getUserName()).toBe("Ana");
	});

	it("returns null when session is expired", () => {
		saveSession(makeSession(makeToken(-1)));
		expect(getUserId()).toBeNull();
		expect(getUserName()).toBeNull();
	});
});
