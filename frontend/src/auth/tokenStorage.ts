import * as SecureStore from "expo-secure-store";
import { Platform } from "react-native";

/**
 * expo-secure-store no está disponible en web. Ahí caemos a localStorage (solo se usa
 * durante `expo start --web` en desarrollo; la app de producción es Android).
 */
const memoryFallback = new Map<string, string>();

function webStorage(): Storage | null {
  try {
    return typeof localStorage !== "undefined" ? localStorage : null;
  } catch {
    return null;
  }
}

export async function getItem(key: string): Promise<string | null> {
  if (Platform.OS === "web") {
    return webStorage()?.getItem(key) ?? memoryFallback.get(key) ?? null;
  }
  return SecureStore.getItemAsync(key);
}

export async function setItem(key: string, value: string): Promise<void> {
  if (Platform.OS === "web") {
    const storage = webStorage();
    if (storage) storage.setItem(key, value);
    else memoryFallback.set(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value);
}

export async function deleteItem(key: string): Promise<void> {
  if (Platform.OS === "web") {
    webStorage()?.removeItem(key);
    memoryFallback.delete(key);
    return;
  }
  await SecureStore.deleteItemAsync(key);
}
