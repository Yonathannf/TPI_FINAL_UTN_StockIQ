import {
  ArrowLeftRight,
  ChartColumn,
  Layers,
  LayoutGrid,
  Package,
  Settings2,
  Users,
  Warehouse,
} from 'lucide-react';

/**
 * Secciones de la app y roles que pueden verlas (RF-02, RN-34, RN-35).
 * El menú, las rutas y la pantalla de inicio se arman a partir de esta lista.
 */
const TODOS = ['ENCARGADO', 'OPERARIO'];
const ENCARGADO = ['ENCARGADO'];

export const INICIO = { ruta: '/', titulo: 'Inicio', icono: LayoutGrid };

export const SECCIONES = [
  {
    grupo: 'Operación',
    items: [
      {
        ruta: '/stock',
        titulo: 'Stock',
        icono: Layers,
        roles: TODOS,
        descripcion: 'Stock por depósito, stock en tránsito y transferencias pendientes.',
      },
      {
        ruta: '/movimientos',
        titulo: 'Movimientos',
        icono: ArrowLeftRight,
        roles: TODOS,
        descripcion: 'Registro de entradas, salidas y transferencias, e historial con filtros.',
      },
    ],
  },
  {
    grupo: 'Análisis',
    items: [
      {
        ruta: '/dashboard',
        titulo: 'Dashboard',
        icono: ChartColumn,
        roles: ENCARGADO,
        descripcion: 'Alertas de reposición, clasificación ABC, rotación y evolución de movimientos.',
      },
    ],
  },
  {
    grupo: 'Administración',
    items: [
      {
        ruta: '/catalogo',
        titulo: 'Catálogo',
        icono: Package,
        roles: ENCARGADO,
        descripcion: 'Productos, categorías y proveedores.',
      },
      {
        ruta: '/depositos',
        titulo: 'Depósitos',
        icono: Warehouse,
        roles: ENCARGADO,
        descripcion: 'Alta, edición y baja de depósitos.',
      },
      {
        ruta: '/usuarios',
        titulo: 'Usuarios',
        icono: Users,
        roles: ENCARGADO,
        descripcion: 'Usuarios del sistema y sus roles.',
      },
      {
        ruta: '/configuracion',
        titulo: 'Configuración',
        icono: Settings2,
        roles: ENCARGADO,
        descripcion: 'Parámetros de cálculo: ventana de consumo, días de cobertura y cortes ABC.',
      },
    ],
  },
];

/** Secciones visibles para un rol, sin los grupos que quedan vacíos. */
export function seccionesPara(rol) {
  return SECCIONES.map((g) => ({ ...g, items: g.items.filter((i) => i.roles.includes(rol)) })).filter(
    (g) => g.items.length > 0
  );
}

/** Grupo y sección de una ruta, para el encabezado de la barra superior. */
export function ubicacionDe(ruta) {
  for (const g of SECCIONES) {
    const item = g.items.find((i) => ruta.startsWith(i.ruta));
    if (item) return { grupo: g.grupo, titulo: item.titulo };
  }
  return { grupo: 'Panel de control', titulo: 'Tu inventario, de un vistazo' };
}

export const NOMBRE_ROL = {
  ENCARGADO: 'Encargado',
  OPERARIO: 'Operario',
};

export function iniciales(usuario) {
  return `${usuario.nombre?.[0] ?? ''}${usuario.apellido?.[0] ?? ''}`.toUpperCase();
}
