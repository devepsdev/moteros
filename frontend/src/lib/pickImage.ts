import * as uploadsApi from "@/api/uploads";
import * as ImagePicker from "expo-image-picker";

/**
 * Abre la galería, sube la imagen elegida y devuelve su URL relativa en el servidor.
 * Devuelve null si el usuario cancela o deniega el permiso.
 */
export async function elegirYSubirImagen(aspect: [number, number] = [4, 3]): Promise<string | null> {
  const permiso = await ImagePicker.requestMediaLibraryPermissionsAsync();
  if (!permiso.granted) return null;

  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: ["images"],
    allowsEditing: true,
    aspect,
    // El backend admite hasta 8 MB; 0.7 deja las fotos de móvil muy por debajo.
    quality: 0.7,
  });
  if (result.canceled || result.assets.length === 0) return null;

  const asset = result.assets[0];
  const subido = await uploadsApi.subirImagen(asset.uri, asset.mimeType, asset.fileName);
  return subido.url;
}
