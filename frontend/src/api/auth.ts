import type {
  CambioPasswordRequest,
  LoginRequest,
  LoginResponse,
  RegistroRequest,
  RestablecerPasswordRequest,
} from "@/types/dto";
import { request } from "./client";

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>("/api/auth/login", { method: "POST", body: data, auth: false });
}

export function registro(data: RegistroRequest): Promise<LoginResponse> {
  return request<LoginResponse>("/api/auth/registro", { method: "POST", body: data, auth: false });
}

export function logout(refreshToken: string): Promise<void> {
  return request<void>("/api/auth/logout", { method: "POST", body: { refreshToken }, auth: false });
}

export function recuperarPassword(email: string): Promise<void> {
  return request<void>("/api/auth/recuperar-password", { method: "POST", body: { email }, auth: false });
}

export function restablecerPassword(data: RestablecerPasswordRequest): Promise<void> {
  return request<void>("/api/auth/restablecer-password", { method: "POST", body: data, auth: false });
}

export function cambiarPassword(data: CambioPasswordRequest): Promise<void> {
  return request<void>("/api/auth/password", { method: "PATCH", body: data });
}
