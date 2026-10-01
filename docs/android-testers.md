# Diagnosticos de testers (desde la compilacion 0.3.5)

El launcher incorpora **Enviar crash o log**. El telefono prepara un ZIP local
con la version de la app, modelo, Android, GPU/Vulkan, ajustes del launcher,
registros recientes de esta app y el historial de cierres que Android permite
consultar. Si esta disponible, incluye la traza de un cierre nativo o ANR.
Tambien conserva la ultima excepcion Java.

- **Correo** abre el selector para compartir el ZIP con el destinatario
  `daniebatuani@gmail.com`, asunto y texto inicial. Selecciona tu app de correo,
  describe lo sucedido y pulsa Enviar.
- **GitHub** pide guardar el ZIP y abre un nuevo issue en
  https://github.com/codepdbh/nfsmw-android/issues/new con los datos basicos
  rellenados. Inicia sesion si es necesario, describe como reproducir el fallo
  y adjunta el ZIP guardado antes de publicar el issue.
- **Guardar ZIP** permite guardar el informe sin abrir correo ni GitHub.

Si el juego se cierra, vuelve a abrir el launcher y usa el boton antes de
intentar varias partidas nuevas. Si se queda colgado, cierra la app desde
Android, vuelve a abrirla y genera el informe. Indica la edicion del juego y
los pasos para reproducir el problema. El registro no siempre contiene una
traza: depende de que Android la conserve y de como termino el proceso.

Los registros se acotan conservando el principio y el final. La app no adjunta
archivos del juego, partidas, identificadores del telefono ni registros de
otras apps. El ZIP no se envia automaticamente. GitHub no recibe adjuntos
desde un enlace: el tester adjunta el archivo en el formulario.

## Redmi Note 8 revisado

En el dispositivo conectado el 1 de octubre de 2026 se comprobaron:

- GPU Adreno 610, controlador Qualcomm del 25/09/2020, Vulkan 1.1.128.
- `shaderInt64 = false`; no anuncia `VK_EXT_descriptor_indexing` y faltan
  `runtimeDescriptorArray`, `descriptorBindingPartiallyBound`,
  `descriptorBindingSampledImageUpdateAfterBind` y
  `descriptorBindingUpdateUnusedWhilePending`.
- Anuncia buffer device address como extension, pero la interfaz nativa del
  SDK actualmente la habilita junto con el resto de funciones de Vulkan 1.2.
- BC1, BC2, BC3, BC4 y BC5 no estan soportados como imagenes muestreadas.
- El ejecutable y la biblioteca de shaders coinciden por SHA256 con la copia
  PAL Espana utilizada en las pruebas anteriores.
- El arranque llega a crear Vulkan, pero el renderer registra:
  `C6: el dispositivo Vulkan no tiene shaderInt64: no se dibuja`.
- El kernel 4.14 ignora `MAP_FIXED_NOREPLACE` y puede devolver otra direccion.
  El runtime aceptaba esa direccion como una reserva fija correcta, dejando
  protegida la pila guest reutilizada. Se corrige comprobando la direccion
  devuelta, descartando la reserva desplazada y habilitando la region original
  cuando ya esta reservada. La prueba nativa fallo antes y paso despues en
  este telefono: commit fijo, colisiones y reutilizacion de paginas protegidas.
- Una excepcion no atendida ahora sigue el cierre normal de Android; antes
  el manejador retornaba y repetia la misma instruccion fallida sin terminar.
  Se verificaron en el telefono los casos atendido, delegado y fatal.

Se conserva el cambio del usuario a `-march=armv8-a`: permite generar codigo
para procesadores ARMv8.0, pero no resuelve las limitaciones del controlador.
El launcher ahora comprueba las funciones necesarias para el renderizador
nativo y ofrece **Probar compatibilidad** y acceso al informe. BC4/5 no se exigen
en esta comprobacion, porque el Galaxy A55 probado admite BC1/2/3 y carece de
BC4/5.

## Probar el modo de compatibilidad

En el launcher selecciona **Renderizador → Compatibilidad · experimental**,
o acepta **Probar compatibilidad** cuando se detecten funciones nativas
ausentes. Pulsa **Jugar**. Este modo no necesita generar la biblioteca de
shaders del renderizador nativo.

Utiliza el backend Xenos con descriptores convencionales, render targets FBO
y conversion de las texturas BC no soportadas. Desactiva las sustituciones
nativas de D3D, materiales, matrices, escenarios y render sin mosaico para
conservar el camino original del juego. La compilacion de shaders es sincrona.
Estas decisiones priorizan conseguir el arranque y pueden provocar pausas.

Con este modo y la correccion de memoria, el Redmi probado reproduce los
videos de inicio y el usuario confirma que llega a conducir y que el coche
gira con el joystick, aunque funciona con mucha lentitud. Tambien se observaron esperas prolongadas de la
GPU y pantallas negras posteriores: el soporte sigue siendo experimental.
No se ha validado todavia en una carrera completa ni en Helio G99/G200.
Los testers de esos dispositivos deben adjuntar su ZIP indicando el modo
utilizado y la ultima pantalla que funciono.

El joystick tactil puede utilizarse incluso con **Inclinar** activado:
mientras se mantiene el dedo, manda el joystick; al soltarlo vuelve la
inclinacion. Esto corrige que el ajuste de inclinacion bloqueara el arrastre.
