import * as chatApi from "@/api/chat";
import { ConversacionView } from "@/components/ConversacionView";
import { LoadingView } from "@/components/ui/ListFooter";
import { Screen } from "@/components/ui/Screen";
import { useAsync } from "@/lib/useAsync";
import { useLocalSearchParams } from "expo-router";

/**
 * Abre el chat con un usuario desde su perfil. El backend no tiene "conversación con X",
 * así que se busca entre las conversaciones recientes; si no está, se empieza una nueva
 * (se crea en el servidor al enviar el primer mensaje).
 */
export default function ChatConUsuarioScreen() {
  const { usuarioUuid, nombre, foto } = useLocalSearchParams<{ usuarioUuid: string; nombre?: string; foto?: string }>();

  const existente = useAsync(async () => {
    const pagina = await chatApi.conversaciones(0, 100);
    return pagina.content.find((c) => c.interlocutor.uuid === usuarioUuid)?.uuid ?? null;
  }, [usuarioUuid]);

  if (existente.loading) {
    return (
      <Screen>
        <LoadingView />
      </Screen>
    );
  }

  // Si falla la búsqueda se abre como nueva: al enviar, el backend reutiliza la conversación si existe.
  return <ConversacionView conversacionUuid={existente.data ?? null} interlocutor={{ uuid: usuarioUuid, nombre: nombre ?? "Chat", foto }} />;
}
