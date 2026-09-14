import { ConversacionView } from "@/components/ConversacionView";
import { useLocalSearchParams } from "expo-router";

/** Conversación existente. Los datos del interlocutor llegan como parámetros desde la lista. */
export default function ConversacionScreen() {
  const { uuid, nombre, usuarioUuid, foto } = useLocalSearchParams<{ uuid: string; nombre?: string; usuarioUuid?: string; foto?: string }>();
  return <ConversacionView conversacionUuid={uuid} interlocutor={{ uuid: usuarioUuid ?? "", nombre: nombre ?? "Conversación", foto }} />;
}
