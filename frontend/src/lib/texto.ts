/**
 * Espelho da regra de `NormalizadorTexto` do backend, usada aqui apenas para
 * apontar duplicatas no formulário antes do envio. A fonte de verdade é o
 * backend (e o índice único do banco); esta cópia só antecipa o erro.
 */
export function normalizarTexto(texto: string): string {
  return texto
    .normalize("NFD")
    .replace(/\p{M}+/gu, "")
    .replace(/[-_/]+/g, " ")
    .replace(/[.,;:'"()[\]!?%]+/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .toUpperCase();
}

/** Converte string vazia ou só espaços em null; apara o resto. */
export function vazioParaNulo(texto: string | null | undefined): string | null {
  if (texto == null) return null;
  const aparado = texto.trim();
  return aparado === "" ? null : aparado;
}
