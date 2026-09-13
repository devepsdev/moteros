import { ApiError } from "@/api/client";

/** Mensaje general y errores por campo listos para mostrar en un formulario. */
export function describeError(cause: unknown): { message: string; fields: Record<string, string> } {
  if (cause instanceof ApiError) {
    const fields = cause.errores ?? {};
    // En los errores de validación el mensaje general es genérico; se prefiere el del primer campo.
    const first = Object.values(fields)[0];
    const message = cause.message === "Error de validacion" && first ? first : cause.message;
    return { message, fields };
  }
  return { message: "No hemos podido conectar con el servidor. Inténtalo de nuevo.", fields: {} };
}
