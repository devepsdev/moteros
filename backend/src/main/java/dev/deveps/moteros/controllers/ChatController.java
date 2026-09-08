package dev.deveps.moteros.controllers;

import dev.deveps.moteros.controllers.support.PageableFactory;
import dev.deveps.moteros.dto.ApiResponseDTO;
import dev.deveps.moteros.dto.ConversacionResponseDTO;
import dev.deveps.moteros.dto.MensajeRequestDTO;
import dev.deveps.moteros.dto.MensajeResponseDTO;
import dev.deveps.moteros.dto.PagedResponseDTO;
import dev.deveps.moteros.services.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversaciones")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<ConversacionResponseDTO>>> misConversaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageableFactory.of(page, size, "fechaCreacion", "desc", "fechaCreacion");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(chatService.misConversaciones(pageable)),
                "Conversaciones obtenidas correctamente"));
    }

    @GetMapping("/conversaciones/{uuid}")
    public ResponseEntity<ApiResponseDTO<ConversacionResponseDTO>> obtenerConversacion(@PathVariable String uuid) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                chatService.obtenerConversacion(uuid), "Conversacion obtenida correctamente"));
    }

    @GetMapping("/conversaciones/{uuid}/mensajes")
    public ResponseEntity<ApiResponseDTO<PagedResponseDTO<MensajeResponseDTO>>> listarMensajes(
            @PathVariable String uuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Pageable pageable = PageableFactory.of(page, size, "fechaEnvio", "desc", "fechaEnvio");
        return ResponseEntity.ok(ApiResponseDTO.success(
                PagedResponseDTO.of(chatService.listarMensajes(uuid, pageable)),
                "Mensajes obtenidos correctamente"));
    }

    @PostMapping("/conversaciones/{uuid}/mensajes")
    public ResponseEntity<ApiResponseDTO<MensajeResponseDTO>> enviarEnConversacion(
            @PathVariable String uuid, @Valid @RequestBody MensajeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(
                chatService.enviarMensajeEnConversacion(uuid, dto), "Mensaje enviado"));
    }

    @PostMapping("/usuarios/{usuarioUuid}/mensajes")
    public ResponseEntity<ApiResponseDTO<MensajeResponseDTO>> enviarAUsuario(
            @PathVariable String usuarioUuid, @Valid @RequestBody MensajeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDTO.success(
                chatService.enviarMensajeAUsuario(usuarioUuid, dto), "Mensaje enviado"));
    }

    @PatchMapping("/conversaciones/{uuid}/leidos")
    public ResponseEntity<ApiResponseDTO<Void>> marcarLeidos(@PathVariable String uuid) {
        chatService.marcarLeidos(uuid);
        return ResponseEntity.ok(ApiResponseDTO.success(null, "Mensajes marcados como leidos"));
    }

    @GetMapping("/no-leidos")
    public ResponseEntity<ApiResponseDTO<Map<String, Long>>> totalNoLeidos() {
        return ResponseEntity.ok(ApiResponseDTO.success(
                Map.of("total", chatService.totalNoLeidos()), "Total de mensajes no leidos"));
    }
}
