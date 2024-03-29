package es.magicwater.controladores;

import es.magicwater.jpa.Proyecto;
import es.magicwater.jpa.Tarea;
import es.magicwater.jpa.Usuario;
import es.magicwater.repositorios.ProyectoRepositorio;
import es.magicwater.repositorios.TareaRepositorio;
import es.magicwater.repositorios.UsuarioRepositorio;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Controller
public class Controlador {
    @Autowired
    UsuarioRepositorio reUsuario;
    @Autowired
    TareaRepositorio reTarea;
    @Autowired
    ProyectoRepositorio reProyecto;
    @Autowired
    PasswordEncoder encoder;

    private Usuario devolverLogin(Authentication aut) {
        Usuario login;
        if (aut==null) { // No se ha iniciado sesión.
            login = null;
        }
        else { // Se ha iniciado sesión.
            Optional<Usuario> userOpt = reUsuario.findById(aut.getName());
            login = userOpt.get();
        }
        return login;
    }

    @RequestMapping("/")
    public ModelAndView peticionRaiz(Authentication aut) {
        ModelAndView mv = new ModelAndView();
        Usuario login = devolverLogin(aut);
        mv.addObject("login", login);
        mv.setViewName("index");
        return mv;
    }

    @RequestMapping("/login")
    public ModelAndView peticionSesion() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("login");
        return mv;
    }

    @RequestMapping("/supervisor")
    public ModelAndView peticionSupervisor() {
        ModelAndView mv = new ModelAndView();

        List<Proyecto> proyectos = reProyecto.findAll();
        mv.addObject("proyectos", proyectos);

        mv.setViewName("miron");
        return mv;
    }

    @RequestMapping("/trabajador")
    public ModelAndView peticionTrabajador(Authentication aut) {
        ModelAndView mv = new ModelAndView();
        Usuario login = devolverLogin(aut);

        List<Proyecto> proyectos = reProyecto.proyectosUsuario(login.getNif());
        mv.addObject("proyectos", proyectos);

        mv.setViewName("gestortareas");
        return mv;
    }

    @RequestMapping("/denegado")
    public ModelAndView peticionDenegado() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("denegado");
        return mv;
    }

    @RequestMapping("/perfil")
    public ModelAndView peticionPerfil(Authentication aut) {
        ModelAndView mv = new ModelAndView();
        Usuario login = devolverLogin(aut);
        mv.addObject("user", login);
        mv.setViewName("perfil");
        return mv;
    }

    @RequestMapping("/guardarperfil")
    public String peticionGuardarPerfil(Usuario u) {
        reUsuario.save(u);
        return "redirect:/";
    }

    @RequestMapping("/contra")
    public ModelAndView peticionCambiarContraseña() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("contra");
        return mv;
    }

    @RequestMapping("/guardarcontra")
    public String peticionGuardarContraseña(HttpServletRequest request,Authentication aut) {
        String contra = request.getParameter("pw1");
        Usuario login = devolverLogin(aut);
        login.setPassword(encoder.encode(contra));
        reUsuario.save(login);
        return "redirect:/perfil";
    }
    @RequestMapping("/trabajador/tarea/editar")
    public ModelAndView peticionEditarTarea(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView();
        int id = Integer.parseInt(request.getParameter("id"));
        Optional<Tarea> t = reTarea.findById(id);
        Tarea tarea = t.orElse(null);
        // Retorna la tarea si está presente, sino null.
        mv.addObject("tarea", tarea);
        mv.setViewName("editartarea");
        return mv;
    }

    @PostMapping("/trabajador/tarea/editar/guardar")
    public String peticionGuardarTareaEditada(HttpServletRequest request) {
        // Por el momento nos limitamos a recoger los parámetros del formulario.
        String id = request.getParameter("id");
        String titulo = request.getParameter("titulo");
        String descripcion = request.getParameter("descripcion");
        String estado = request.getParameter("estado");
        String inicioprevisto = request.getParameter("inicioprevisto");
        String finprevisto = request.getParameter("finprevisto");
        String inicioreal = request.getParameter("inicioreal");
        String finreal = request.getParameter("finreal");

        System.out.println("Id Tarea: " + id);
        System.out.println("Título: " + titulo);
        System.out.println("Descripción: " + descripcion);
        System.out.println("Estado: " + estado);
        System.out.println("Inicio previsto: " + inicioprevisto);
        System.out.println("Fin previsto: " + finprevisto);
        System.out.println("Inicio real: " + inicioreal);
        System.out.println("Fin real: " + finreal);

        int idTarea = Integer.parseInt(id);
        Tarea tarea = reTarea.findById(idTarea).get();
        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setEstado(estado);

        // Convierte las cadenas de fecha en objetos Date
        Date inicioPrevistoDate = (inicioprevisto.isEmpty()) ? null : Date.valueOf(inicioprevisto);
        Date finPrevistoDate = (finprevisto.isEmpty()) ? null : Date.valueOf(finprevisto);
        Date inicioRealDate = (inicioreal.isEmpty()) ? null : Date.valueOf(inicioreal);
        Date finRealDate = (finreal.isEmpty()) ? null : Date.valueOf(finreal);

        tarea.setInicioprevisto(inicioPrevistoDate);
        tarea.setFinprevisto(finPrevistoDate);
        tarea.setInicioreal(inicioRealDate);
        tarea.setFinreal(finRealDate);

        // Las fechas deberían ser coherentes entre ellas y con el estado.
        // Esto te toca arreglarlo a tí.

        // Guardamos los cambios.
        reTarea.save(tarea);

        return "redirect:/trabajador";
    }
    @GetMapping("/trabajador/tarea/eliminar/{id}")
    public ModelAndView confirmarEliminarTarea(@PathVariable int id) {
        ModelAndView modelAndView = new ModelAndView("eliminar");
        modelAndView.addObject("id", id);
        return modelAndView;
    }

    // Método para eliminar la tarea
    @GetMapping("/trabajador/tarea/eliminar/confirmar/{id}")
    public String eliminarTarea(@PathVariable int id) {
        Optional<Tarea> tareaOptional = reTarea.findById(id);

        if (tareaOptional.isPresent()) {
            Tarea tarea = tareaOptional.get();
            reTarea.delete(tarea);
        }
        return "redirect:/trabajador";
    }


    @GetMapping("/trabajador/tarea/nueva")
    public ModelAndView peticionNuevaTarea(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView();
        Tarea t = new Tarea();
        mv.addObject("tarea", t);
        mv.setViewName("nuevatarea");
        return mv;
    }
    @PostMapping("/trabajador/tarea/nueva/guardar")
    public String peticionGuardarTareaNueva(HttpServletRequest request, Authentication aut) {
        String titulo = request.getParameter("titulo");
        String descripcion = request.getParameter("descripcion");
        String estado = request.getParameter("estado");
        String inicioprevisto = request.getParameter("inicioprevisto");
        String finprevisto = request.getParameter("finprevisto");
        String inicioreal = request.getParameter("inicioreal");
        String finreal = request.getParameter("finreal");

        Usuario login = devolverLogin(aut);
        Tarea tarea = new Tarea();
        tarea.setIdtarea(reTarea.newIdTarea());
        tarea.setUsuario(login);
        tarea.setTitulo(titulo);
        tarea.setDescripcion(descripcion);
        tarea.setEstado(estado);

        // Es una tarea suelta que será asignada al proyecto 0 (tareas sueltas).
        Proyecto p = reProyecto.findById(0).get();
        tarea.setProyecto(p);

        Date inicioPrevisto = (inicioprevisto.isEmpty()) ? null : Date.valueOf(inicioprevisto);
        Date finPrevisto = (finprevisto.isEmpty()) ? null : Date.valueOf(finprevisto);
        tarea.setFinprevisto(inicioPrevisto);
        tarea.setInicioprevisto(finPrevisto);

        Date inicioReal = (inicioreal.isEmpty()) ? null : Date.valueOf(inicioreal);
        Date finReal = (finreal.isEmpty()) ? null : Date.valueOf(finreal);
        tarea.setInicioreal(inicioReal);
        tarea.setFinreal(finReal);

        // Las fechas deberían ser coherentes entre ellas y con el estado.
        // Esto te toca arreglarlo a tí.
        reTarea.save(tarea);

        return "redirect:/trabajador";
    }

    @GetMapping("/trabajador/proyecto/nuevo")
    public ModelAndView peticionNuevoProyecto(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView();
        Proyecto p = new Proyecto();
        mv.addObject("proyecto", p);
        mv.setViewName("nuevoproyecto");
        return mv;
    }
    @RequestMapping(value = "/trabajador/proyecto/nuevo/guardar", method = RequestMethod.POST)
    public String peticionGuardarProyectoNuevo(HttpServletRequest request, Authentication aut) {
        String nombre = request.getParameter("nombre");
        String descripcion = request.getParameter("descripcion");
        String zona = request.getParameter("zona");
        String fecha = request.getParameter("fecha");

        Proyecto proyecto = new Proyecto();
        proyecto.setIdproyecto(reProyecto.newIdProyecto());
        proyecto.setNombre(nombre);
        proyecto.setDescripcion(descripcion);
        proyecto.setZona(zona);

        Date fechaDate = (fecha.isEmpty()) ? null : Date.valueOf(fecha);
        proyecto.setFecha(fechaDate);
        reProyecto.save(proyecto);

        Usuario login = devolverLogin(aut);

        LocalDate fechaLd = LocalDate.from(fechaDate.toLocalDate().atStartOfDay());

        // Creación de la tarea predeterminada Recolección de muestras
        Tarea tarea1 = new Tarea();
        tarea1.setIdtarea(reTarea.newIdTarea());
        tarea1.setTitulo("Recolección de muestras");
        tarea1.setDescripcion("Recolección de muestras en " + zona + " para proyecto " + fecha);
        tarea1.setEstado("Pendiente");
        tarea1.setUsuario(login);
        tarea1.setProyecto(proyecto);
        tarea1.setInicioprevisto(fechaDate);
        tarea1.setFinprevisto(Date.valueOf(fechaLd.plusDays(30)));
        reTarea.save(tarea1);

        // Creación de la tarea predeterminada Análisis de laboratorio
        Tarea tarea2 = new Tarea();
        tarea2.setIdtarea(reTarea.newIdTarea());
        tarea2.setTitulo("Análisis de laboratorio");
        tarea2.setDescripcion("Análisis de laboratorio en " + zona + " para proyecto " + fecha);
        tarea2.setEstado("Pendiente");
        tarea2.setUsuario(login);
        tarea2.setProyecto(proyecto);
        tarea2.setInicioprevisto(fechaDate);
        tarea2.setFinprevisto(Date.valueOf(fechaLd.plusDays(60)));
        reTarea.save(tarea2);

        // Creación de la tarea predeterminada Interpretación de resultados
        Tarea tarea3 = new Tarea();
        tarea3.setIdtarea(reTarea.newIdTarea());
        tarea3.setTitulo("Interpretación de resultados");
        tarea3.setDescripcion("Interpretación de resultados en " + zona + " para proyecto " + fecha);
        tarea3.setEstado("Pendiente");
        tarea3.setUsuario(login);
        tarea3.setProyecto(proyecto);
        tarea3.setInicioprevisto(fechaDate);
        tarea3.setFinprevisto(Date.valueOf(fechaLd.plusDays(90)));
        reTarea.save(tarea3);

        // Creación de la tarea predeterminada Elaboración del informe
        Tarea tarea4 = new Tarea();
        tarea4.setIdtarea(reTarea.newIdTarea());
        tarea4.setTitulo("Elaboración del informe");
        tarea4.setDescripcion("Elaboración de informe en " + zona + " para proyecto " + fecha);
        tarea4.setEstado("Pendiente");
        tarea4.setUsuario(login);
        tarea4.setProyecto(proyecto);
        tarea4.setInicioprevisto(fechaDate);
        tarea4.setFinprevisto(Date.valueOf(fechaLd.plusDays(120)));
        reTarea.save(tarea4);

        return "redirect:/trabajador";
    }
}