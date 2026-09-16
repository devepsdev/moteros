import type { TipoDenuncia } from "@/api/denuncias";
import type { useRouter } from "expo-router";

/** Abre el formulario de denuncia. Con el autor, al terminar se ofrece bloquearlo. */
export function abrirDenuncia(router: ReturnType<typeof useRouter>, tipo: TipoDenuncia, uuid: string, autor?: { uuid: string; nombre: string }) {
  router.push({
    pathname: "/denunciar",
    params: { tipo, uuid, ...(autor ? { autorUuid: autor.uuid, autorNombre: autor.nombre } : {}) },
  });
}
