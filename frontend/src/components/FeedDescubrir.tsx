import * as quedadasApi from "@/api/quedadas";
import * as rutasApi from "@/api/rutas";
import { QuedadaCard } from "@/components/QuedadaCard";
import { RutaCard } from "@/components/RutaCard";
import { Button } from "@/components/ui/Button";
import { Text } from "@/components/ui/Text";
import { useAsync } from "@/lib/useAsync";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { useRouter } from "expo-router";
import { ActivityIndicator, Pressable, View } from "react-native";

const RUTAS = 4;
const QUEDADAS = 3;

/**
 * Lo que ve quien todavía no sigue a nadie: el feed solo trae lo tuyo y lo de tus amigos, así que
 * en una cuenta recién hecha estaría vacío. En su lugar se enseña el catálogo: unas cuantas rutas
 * y las próximas quedadas, que es por donde se empieza a usar la app.
 */
export function FeedDescubrir() {
  const theme = useTheme();
  const router = useRouter();

  const rutas = useAsync(() => rutasApi.listar({}, 0, RUTAS), []);
  const quedadas = useAsync(() => quedadasApi.proximas(0, QUEDADAS), []);

  const cargando = (rutas.loading && !rutas.data) || (quedadas.loading && !quedadas.data);
  const listaRutas = rutas.data?.content ?? [];
  const listaQuedadas = quedadas.data?.content ?? [];

  return (
    <View style={{ gap: theme.spacing.xl, paddingBottom: theme.spacing.xl }}>
      <View style={{ gap: theme.spacing.sm, paddingTop: theme.spacing.md }}>
        <Text variant="title1">Empieza por aquí</Text>
        <Text variant="body" color="inkMuted">
          Tu feed se llenará con lo que publiquéis tú y la gente a la que sigas. Mientras tanto,
          echa un vistazo a lo que hay rodando.
        </Text>
      </View>

      {cargando ? (
        <ActivityIndicator color={theme.colors.accent} style={{ marginVertical: theme.spacing.xxl }} />
      ) : null}

      {listaRutas.length > 0 ? (
        <View style={{ gap: theme.spacing.md }}>
          <Cabecera titulo="Rutas para estrenar" onVerTodas={() => router.push("/rutas")} />
          {listaRutas.map((r) => (
            <RutaCard key={r.uuid} ruta={r} />
          ))}
        </View>
      ) : null}

      {listaQuedadas.length > 0 ? (
        <View style={{ gap: theme.spacing.md }}>
          <Cabecera titulo="Próximas quedadas" onVerTodas={() => router.push("/quedadas")} />
          {listaQuedadas.map((q) => (
            <QuedadaCard key={q.uuid} quedada={q} />
          ))}
        </View>
      ) : null}

      <View style={{ gap: theme.spacing.md }}>
        <Button
          label="Buscar moteros"
          variant="secondary"
          fullWidth
          icon={<Feather name="users" size={16} color={theme.colors.ink} />}
          onPress={() => router.push("/buscar")}
        />
        <Button
          label="Contar tu última salida"
          variant="ghost"
          fullWidth
          icon={<Feather name="edit-3" size={16} color={theme.colors.accent} />}
          onPress={() => router.push("/publicacion/nueva")}
        />
      </View>
    </View>
  );
}

function Cabecera({ titulo, onVerTodas }: { titulo: string; onVerTodas: () => void }) {
  return (
    <View style={{ flexDirection: "row", alignItems: "center", justifyContent: "space-between" }}>
      <Text variant="title2">{titulo}</Text>
      <Pressable onPress={onVerTodas} style={({ pressed }) => ({ opacity: pressed ? 0.6 : 1 })}>
        <Text variant="captionMedium" color="accent">
          Ver todas
        </Text>
      </Pressable>
    </View>
  );
}
