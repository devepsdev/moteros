// Amplía app.json con valores que no deben ir en el repositorio.
// La clave de Google Maps (Maps SDK for Android) se lee de GOOGLE_MAPS_API_KEY al compilar
// (variable de entorno local o secreto de EAS). En Expo Go no hace falta.
module.exports = ({ config }) => ({
  ...config,
  plugins: [
    ...config.plugins,
    ["react-native-maps", { androidGoogleMapsApiKey: process.env.GOOGLE_MAPS_API_KEY }],
  ],
});
