import { api } from "../../app/lib/api";

/*
 * Testes da camada HTTP — verifica que o cliente da API adiciona o token
 * e trata erros do backend correctamente.
 */

// mock do módulo auth para controlar o token nos testes
jest.mock("../../app/lib/auth", () => ({
	getToken: jest.fn(),
	clearSession: jest.fn(),
}));

import { getToken } from "../../app/lib/auth";

const mockGetToken = getToken as jest.Mock;

// substitui fetch global por um mock controlável
const mockFetch = jest.fn();
global.fetch = mockFetch;

// helper que cria uma response falsa com o formato da Fetch API
function makeResponse(status: number, body?: unknown) {
	return {
		ok: status >= 200 && status < 300,
		status,
		json: jest.fn().mockResolvedValue(body ?? {}),
	};
}

beforeEach(() => {
	mockFetch.mockReset();
	mockGetToken.mockReturnValue(null);
});

describe("api.get", () => {
	it("envia o Authorization header quando existe token", async () => {
		mockGetToken.mockReturnValue("meu-token");
		mockFetch.mockResolvedValue(makeResponse(200, { id: 1 }));

		await api.get("/test");

		const [, options] = mockFetch.mock.calls[0];
		expect(options.headers["Authorization"]).toBe("Bearer meu-token");
	});

	it("não envia Authorization quando não há sessão", async () => {
		mockFetch.mockResolvedValue(makeResponse(200, {}));

		await api.get("/test");

		const [, options] = mockFetch.mock.calls[0];
		expect(options.headers["Authorization"]).toBeUndefined();
	});

	it("devolve os dados quando o response é 200", async () => {
		mockFetch.mockResolvedValue(makeResponse(200, { name: "Ana" }));

		const result = await api.get<{ name: string }>("/users/1");

		expect(result.name).toBe("Ana");
	});

	it("lança erro com a mensagem do backend quando response não é ok", async () => {
		mockFetch.mockResolvedValue(makeResponse(409, { message: "Pedido duplicado" }));

		await expect(api.get("/test")).rejects.toThrow("Pedido duplicado");
	});

	it("em 204 devolve undefined sem tentar fazer json()", async () => {
		mockFetch.mockResolvedValue(makeResponse(204));

		const result = await api.delete("/vacation-requests/1");

		expect(result).toBeUndefined();
	});
});

describe("api.post", () => {
	it("envia o body como JSON com o método POST", async () => {
		mockFetch.mockResolvedValue(makeResponse(201, { id: 99 }));

		await api.post("/vacation-requests", { startDate: "2026-06-01", endDate: "2026-06-10" });

		const [, options] = mockFetch.mock.calls[0];
		expect(options.method).toBe("POST");
		expect(options.body).toBe(JSON.stringify({ startDate: "2026-06-01", endDate: "2026-06-10" }));
	});
});
