#include "../app/src/nfsmw_shader_library.h"
#include "nfsmw_contenedor.h"
#include <algorithm>
#include <cstdio>
#include <fstream>
#include <stdexcept>

namespace fs = std::filesystem;
std::vector<uint8_t> Leer(const fs::path& ruta) {
  std::ifstream f(ruta, std::ios::binary | std::ios::ate);
  if (!f || f.tellg() < 0 || f.tellg() > 64 * 1024 * 1024)
    throw std::runtime_error("Input file unreadable or too large: " + ruta.string());
  std::vector<uint8_t> d(static_cast<size_t>(f.tellg()));
  f.seekg(0);
  if (!f.read(reinterpret_cast<char*>(d.data()), d.size())) throw std::runtime_error("Incomplete read");
  return d;
}

int main(int argc, char** argv) try {
  if (argc != 4) throw std::runtime_error("Usage: nfsmw_empaquetar <containers> <validated spirv> <new output>");
  const fs::path originales = argv[1], compilados = argv[2], salida = argv[3];
  if (fs::exists(salida)) throw std::runtime_error("The output already exists");
  std::vector<fs::path> rutas;
  for (const auto& e : fs::directory_iterator(originales))
    if (e.is_regular_file() && e.path().extension() == ".bin") rutas.push_back(e.path());
  std::sort(rutas.begin(), rutas.end());
  std::vector<nfsmw::native::Shader> shaders;
  for (const auto& ruta : rutas) {
    nfsmw::native::Shader s;
    s.original = Leer(ruta);
    nfsmw::Flujo flujo;
    (void)nfsmw::Convertir2005(s.original, flujo);
    auto spv = Leer(compilados / (ruta.stem().string() + ".spv"));
    if (spv.size() % 4) throw std::runtime_error("Misaligned SPIR-V");
    for (size_t i = 0; i < spv.size(); i += 4)
      s.spirv.push_back(uint32_t(spv[i]) | uint32_t(spv[i+1]) << 8 |
                       uint32_t(spv[i+2]) << 16 | uint32_t(spv[i+3]) << 24);
    shaders.push_back(std::move(s));
  }
  auto paquete = nfsmw::native::EmpaquetarShaders(std::move(shaders));
  nfsmw::native::BibliotecaShaders biblioteca;
  biblioteca.Cargar(paquete);
  for (const auto& ruta : rutas)
    if (!biblioteca.Buscar(Leer(ruta))) throw std::runtime_error("A shader is not found after packing");
  std::ofstream f(salida, std::ios::binary);
  if (!f.write(reinterpret_cast<const char*>(paquete.data()), paquete.size()) || !f.flush())
    throw std::runtime_error("Could not write the whole package");
  std::printf("%zu containers -> %zu unique shaders, %zu bytes; all recoverable\n",
              rutas.size(), biblioteca.shaders().size(), paquete.size());
  return 0;
} catch (const std::exception& e) {
  std::fprintf(stderr, "%s\n", e.what());
  return 1;
}
