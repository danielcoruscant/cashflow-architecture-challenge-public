import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

type TestOpenApiConfig = {
  BASE: string;
  TOKEN: string | (() => Promise<string>) | undefined;
};

async function loadAuth(overrides: {
  clientId?: string;
  apiScope?: string;
  tenantId?: string;
  apiBaseUrl?: string;
} = {}) {
  const env = {
    clientId: '',
    apiScope: '',
    tenantId: '',
    apiBaseUrl: '',
    ...overrides,
  };
  vi.stubEnv('VITE_ENTRA_CLIENT_ID', env.clientId);
  vi.stubEnv('VITE_ENTRA_API_SCOPE', env.apiScope);
  vi.stubEnv('VITE_ENTRA_TENANT_ID', env.tenantId);
  vi.stubEnv('VITE_API_BASE_URL', env.apiBaseUrl);

  const client = {
    getActiveAccount: vi.fn(),
    getAllAccounts: vi.fn(),
    acquireTokenSilent: vi.fn(),
  };
  const PublicClientApplication = vi.fn(function PublicClientApplicationMock() {
    return client;
  });
  const lancamentosOpenApi: TestOpenApiConfig = { BASE: '', TOKEN: undefined };
  const consolidadoOpenApi: TestOpenApiConfig = { BASE: '', TOKEN: undefined };

  vi.doMock('@azure/msal-browser', () => ({ PublicClientApplication }));
  vi.doMock('../generated/lancamentos', () => ({ OpenAPI: lancamentosOpenApi }));
  vi.doMock('../generated/consolidado', () => ({ OpenAPI: consolidadoOpenApi }));

  return {
    auth: await import('../services/auth'),
    client,
    PublicClientApplication,
    lancamentosOpenApi,
    consolidadoOpenApi,
  };
}

describe('serviço de autenticação', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it('rejeita configuração ausente e mantém a URL local padrão dos clientes', async () => {
    const { auth, PublicClientApplication, lancamentosOpenApi, consolidadoOpenApi } = await loadAuth();

    expect(auth.hasAuthConfiguration).toBe(false);
    expect(auth.msalInstance).toBeNull();
    expect(auth.loginRequest.scopes).toEqual(['openid', 'profile']);
    expect(PublicClientApplication).not.toHaveBeenCalled();
    await expect(auth.getAccessToken()).rejects.toThrow(
      'Configure VITE_ENTRA_CLIENT_ID e VITE_ENTRA_API_SCOPE para acessar a API.',
    );

    auth.configureGeneratedClients();
    expect(lancamentosOpenApi.BASE).toBe('/api/v1');
    expect(consolidadoOpenApi.BASE).toBe('/api/v1');
    expect(lancamentosOpenApi.TOKEN).toBe(auth.getAccessToken);
    expect(consolidadoOpenApi.TOKEN).toBe(auth.getAccessToken);
  });

  it('usa configuração normalizada, conta ativa e URL configurada', async () => {
    const { auth, client, PublicClientApplication, lancamentosOpenApi, consolidadoOpenApi } =
      await loadAuth({
        clientId: '  client-1  ',
        apiScope: '  api://cashflow/read  ',
        tenantId: ' tenant-1 ',
        apiBaseUrl: ' https://api.example.test/api/v1 ',
      });
    const account = { username: 'user@example.test' };
    client.getActiveAccount.mockReturnValue(account);
    client.acquireTokenSilent.mockResolvedValue({ accessToken: 'access-token' });

    expect(auth.hasAuthConfiguration).toBe(true);
    expect(auth.loginRequest.scopes).toEqual(['openid', 'profile', 'api://cashflow/read']);
    expect(PublicClientApplication).toHaveBeenCalledWith(expect.objectContaining({
      auth: expect.objectContaining({
        clientId: 'client-1',
        authority: 'https://login.microsoftonline.com/tenant-1',
        redirectUri: window.location.origin,
        postLogoutRedirectUri: window.location.origin,
      }),
      cache: { cacheLocation: 'sessionStorage' },
    }));
    await expect(auth.getAccessToken()).resolves.toBe('access-token');
    expect(client.getAllAccounts).not.toHaveBeenCalled();
    expect(client.acquireTokenSilent).toHaveBeenCalledWith({
      scopes: ['api://cashflow/read'],
      account,
    });

    auth.configureGeneratedClients();
    expect(lancamentosOpenApi.BASE).toBe('https://api.example.test/api/v1');
    expect(consolidadoOpenApi.BASE).toBe('https://api.example.test/api/v1');
    auth.configureGeneratedClients('https://override.example.test');
    expect(lancamentosOpenApi.BASE).toBe('https://override.example.test');
    expect(consolidadoOpenApi.BASE).toBe('https://override.example.test');
  });

  it('usa a primeira conta disponível quando não há conta ativa', async () => {
    const { auth, client } = await loadAuth({ clientId: 'client-1', apiScope: 'api://cashflow/read' });
    const account = { username: 'fallback@example.test' };
    client.getActiveAccount.mockReturnValue(null);
    client.getAllAccounts.mockReturnValue([account]);
    client.acquireTokenSilent.mockResolvedValue({ accessToken: 'fallback-token' });

    await expect(auth.getAccessToken()).resolves.toBe('fallback-token');
    expect(client.acquireTokenSilent).toHaveBeenCalledWith({
      scopes: ['api://cashflow/read'],
      account,
    });
  });

  it('solicita login quando não há contas disponíveis', async () => {
    const { auth, client } = await loadAuth({ clientId: 'client-1', apiScope: 'api://cashflow/read' });
    client.getActiveAccount.mockReturnValue(null);
    client.getAllAccounts.mockReturnValue([]);

    await expect(auth.getAccessToken()).rejects.toThrow('Faça login com uma conta Microsoft para continuar.');
    expect(client.acquireTokenSilent).not.toHaveBeenCalled();
  });
});
