import { Configuration, PublicClientApplication } from '@azure/msal-browser';
import type { IPublicClientApplication } from '@azure/msal-browser';
import { OpenAPI as LancamentosOpenAPI } from '../generated/lancamentos';
import { OpenAPI as ConsolidadoOpenAPI } from '../generated/consolidado';

const clientId = import.meta.env.VITE_ENTRA_CLIENT_ID?.trim() ?? '';
const apiScope = import.meta.env.VITE_ENTRA_API_SCOPE?.trim() ?? '';
const tenantId = import.meta.env.VITE_ENTRA_TENANT_ID?.trim() || 'organizations';

export const hasAuthConfiguration = Boolean(clientId && apiScope);
export const requiredAuthVariables = ['VITE_ENTRA_CLIENT_ID', 'VITE_ENTRA_API_SCOPE'] as const;

const msalConfiguration: Configuration = {
  auth: {
    clientId: clientId || '00000000-0000-0000-0000-000000000000',
    authority: `https://login.microsoftonline.com/${tenantId}`,
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
  },
  cache: { cacheLocation: 'sessionStorage' },
};

export const msalInstance: IPublicClientApplication | null = hasAuthConfiguration
  ? new PublicClientApplication(msalConfiguration)
  : null;

export const loginRequest = {
  scopes: ['openid', 'profile', apiScope].filter(Boolean),
};

export async function getAccessToken(): Promise<string> {
  if (!msalInstance || !apiScope) {
    throw new Error('Configure VITE_ENTRA_CLIENT_ID e VITE_ENTRA_API_SCOPE para acessar a API.');
  }

  const account = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0];
  if (!account) {
    throw new Error('Faça login com uma conta Microsoft para continuar.');
  }

  const result = await msalInstance.acquireTokenSilent({ scopes: [apiScope], account });
  return result.accessToken;
}

export function configureGeneratedClients(baseUrl = import.meta.env.VITE_API_BASE_URL?.trim() || '/api/v1') {
  LancamentosOpenAPI.BASE = baseUrl;
  LancamentosOpenAPI.TOKEN = getAccessToken;
  ConsolidadoOpenAPI.BASE = baseUrl;
  ConsolidadoOpenAPI.TOKEN = getAccessToken;
}
