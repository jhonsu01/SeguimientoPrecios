package com.jhonsu.seguimientoprecios.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Producto rastreable (Guia.md - Tabla Productos). */
@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val categoria: String = "General",
    val tipo: String = "",
    val codigoBarras: String? = null,
    val unidadMedida: String = "unidad",
    val creadoEn: Long = System.currentTimeMillis()
)

/** Tienda/establecimiento (Guia.md - Tabla Tiendas). */
@Entity(tableName = "tiendas")
data class Tienda(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val ubicacion: String? = null
)

/** Registro historico de precio de un producto (Guia.md - Tabla Precios). */
@Entity(
    tableName = "precios",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productoId")]
)
data class Precio(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productoId: String,
    val precio: Double,
    val cantidad: Double = 1.0,
    val tipoPrecio: String = "unitario",
    val tienda: String = "",
    val fecha: Long = System.currentTimeMillis()
)

/** Inventario domestico "Mi Alacena" (Guia.md - Tabla Alacena). Una fila por producto. */
@Entity(tableName = "alacena")
data class Alacena(
    @PrimaryKey val productoId: String,
    val cantidadActual: Double = 0.0,
    val cantidadMinima: Double = 1.0
)
