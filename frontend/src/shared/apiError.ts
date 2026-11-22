export function getApiErrorMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'body' in error) {
    const body = error.body;
    if (typeof body === 'object' && body !== null && 'mensagem' in body && typeof body.mensagem === 'string') {
      return body.mensagem;
    }
  }
  return error instanceof Error ? error.message : 'Ocorreu um erro inesperado. Tente novamente.';
}
