import { AuthProvider, useAuth } from "@/auth/AuthContext";
import { useTheme } from "@/theme";
import { BarlowCondensed_600SemiBold, BarlowCondensed_700Bold } from "@expo-google-fonts/barlow-condensed";
import { Inter_400Regular, Inter_500Medium, Inter_600SemiBold } from "@expo-google-fonts/inter";
import { useFonts } from "expo-font";
import { Stack } from "expo-router";
import * as SplashScreen from "expo-splash-screen";
import { StatusBar } from "expo-status-bar";
import { useEffect } from "react";
import { SafeAreaProvider } from "react-native-safe-area-context";

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({
    BarlowCondensed_600SemiBold,
    BarlowCondensed_700Bold,
    Inter_400Regular,
    Inter_500Medium,
    Inter_600SemiBold,
  });

  if (!fontsLoaded && !fontError) return null;

  return (
    <SafeAreaProvider>
      <AuthProvider>
        <RootNavigator />
      </AuthProvider>
    </SafeAreaProvider>
  );
}

/**
 * Toda la app requiere sesión (la API no tiene endpoints públicos salvo los de acceso).
 * Con Stack.Protected, al cambiar `isAuthenticated` el router redirige solo: a las
 * pestañas al iniciar sesión y a /acceso al cerrarla.
 */
function RootNavigator() {
  const theme = useTheme();
  const { isAuthenticated, isLoading, user } = useAuth();

  useEffect(() => {
    if (!isLoading) SplashScreen.hideAsync();
  }, [isLoading]);

  // Mientras se comprueba la sesión guardada se mantiene el splash.
  if (isLoading) return null;

  return (
    <>
      <StatusBar style={theme.isDark ? "light" : "dark"} />
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: theme.colors.background } }}>
        <Stack.Protected guard={isAuthenticated}>
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="ruta/[uuid]" />
          <Stack.Screen name="ruta/nueva" />
          <Stack.Screen name="valorar/[uuid]" options={{ presentation: "modal" }} />
          <Stack.Screen name="publicacion/nueva" options={{ presentation: "modal" }} />
          <Stack.Screen name="publicacion/[uuid]" />
          <Stack.Screen name="moto/editar" />
          <Stack.Screen name="usuario/[uuid]" />
          <Stack.Screen name="cuenta/datos" />
          <Stack.Screen name="cuenta/contrasena" />
          <Stack.Screen name="cuenta/eliminar" />
          <Stack.Screen name="quedada/[uuid]" />
          <Stack.Screen name="quedada/nueva" />
          <Stack.Screen name="chat/[uuid]" />
          <Stack.Screen name="chat/usuario/[usuarioUuid]" />
          <Stack.Screen name="amigos" />
          <Stack.Screen name="buscar" />
          <Stack.Screen name="notificaciones" />
        </Stack.Protected>

        {/* Solo administradores; el backend exige además ROLE_ADMIN en /api/admin. */}
        <Stack.Protected guard={isAuthenticated && user?.rol === "admin"}>
          <Stack.Screen name="admin/index" />
          <Stack.Screen name="admin/usuarios" />
        </Stack.Protected>

        <Stack.Protected guard={!isAuthenticated}>
          <Stack.Screen name="acceso" />
          <Stack.Screen name="recuperar" />
        </Stack.Protected>
      </Stack>
    </>
  );
}
