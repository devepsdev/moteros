import { useCallback, useEffect, useState } from "react";

interface AsyncState<T> {
  data: T | null;
  error: Error | null;
  loading: boolean;
  reload: () => void;
}

/**
 * Carga datos de la API y expone estado de carga/error/recarga.
 * Descarta respuestas de peticiones ya obsoletas al cambiar las dependencias.
 */
export function useAsync<T>(load: () => Promise<T>, deps: unknown[]): AsyncState<T> {
  const [reloadToken, setReloadToken] = useState(0);
  const requestKey = `${JSON.stringify(deps)}|${reloadToken}`;

  const [state, setState] = useState<Omit<AsyncState<T>, "reload">>({ data: null, error: null, loading: true });
  const [activeKey, setActiveKey] = useState(requestKey);

  // "Cargando" se marca durante el render (no en el efecto) y se conservan los datos
  // anteriores para que una recarga no vacíe la pantalla.
  if (requestKey !== activeKey) {
    setActiveKey(requestKey);
    setState((previous) => ({ data: previous.data, error: null, loading: true }));
  }

  useEffect(() => {
    let active = true;
    load()
      .then((result) => {
        if (active) setState({ data: result, error: null, loading: false });
      })
      .catch((cause: unknown) => {
        if (active) {
          setState({ data: null, error: cause instanceof Error ? cause : new Error(String(cause)), loading: false });
        }
      });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey]);

  const reload = useCallback(() => setReloadToken((token) => token + 1), []);

  return { ...state, reload };
}
