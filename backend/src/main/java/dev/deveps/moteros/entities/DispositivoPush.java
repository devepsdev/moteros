package dev.deveps.moteros.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Tabla `dispositivos_push`: donde recibe avisos un usuario con la app cerrada. */
@Entity
@Table(name = "dispositivos_push")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispositivoPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Dueno del dispositivo: el ultimo que inicio sesion en el. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Token de Expo (ExponentPushToken[...]), unico por instalacion. */
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(length = 20)
    private String plataforma;

    @CreationTimestamp
    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    /** Ultima vez que la app confirmo el token: sirve para limpiar instalaciones muertas. */
    @Column(name = "fecha_uso", nullable = false)
    private LocalDateTime fechaUso;
}
