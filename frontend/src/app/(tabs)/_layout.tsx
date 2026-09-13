import { useNoLeidos, useSondeoNoLeidos } from "@/lib/noLeidos";
import { useTheme } from "@/theme";
import Feather from "@expo/vector-icons/Feather";
import { Tabs } from "expo-router";

type FeatherName = React.ComponentProps<typeof Feather>["name"];

const TABS: { name: string; title: string; icon: FeatherName }[] = [
  { name: "index", title: "Inicio", icon: "home" },
  { name: "rutas", title: "Rutas", icon: "map" },
  { name: "quedadas", title: "Quedadas", icon: "calendar" },
  { name: "chat", title: "Chat", icon: "message-circle" },
  { name: "perfil", title: "Perfil", icon: "user" },
];

export default function TabsLayout() {
  const theme = useTheme();
  useSondeoNoLeidos();
  const noLeidos = useNoLeidos();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: theme.colors.accent,
        tabBarInactiveTintColor: theme.colors.inkFaint,
        tabBarStyle: {
          backgroundColor: theme.colors.surface,
          borderTopWidth: 1,
          borderTopColor: theme.colors.border,
          elevation: 0,
        },
        tabBarLabelStyle: { fontFamily: theme.fontFamily.sansSemibold, fontSize: 11, letterSpacing: 0.3 },
        tabBarBadgeStyle: { backgroundColor: theme.colors.accent, color: "#FFFFFF", fontFamily: theme.fontFamily.sansSemibold, fontSize: 10 },
        sceneStyle: { backgroundColor: theme.colors.background },
      }}
    >
      {TABS.map(({ name, title, icon }) => (
        <Tabs.Screen
          key={name}
          name={name}
          options={{
            title,
            tabBarIcon: ({ color }) => <Feather name={icon} size={21} color={color} />,
            tabBarBadge: name === "chat" && noLeidos > 0 ? (noLeidos > 99 ? "99+" : noLeidos) : undefined,
          }}
        />
      ))}
    </Tabs>
  );
}
