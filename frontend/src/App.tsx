import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { EstoqueProvider } from "@/app/EstoqueContext";
import { AppLayout } from "@/layouts/AppLayout";
import { AlertasPage } from "@/pages/AlertasPage";
import { DashboardPage } from "@/pages/DashboardPage";
import { EstoquePage } from "@/pages/EstoquePage";
import { LoteDetailPage } from "@/pages/LoteDetailPage";
import { LotesPage } from "@/pages/LotesPage";
import { MovimentacoesPage } from "@/pages/MovimentacoesPage";
import { NovaMovimentacaoPage } from "@/pages/NovaMovimentacaoPage";
import { StatusPage } from "@/pages/StatusPage";
import { RepositorioPage } from "@/pages/RepositorioPage";
import { PlantaDetailPage } from "@/pages/PlantaDetailPage";
import { PlantaFormPage } from "@/pages/PlantaFormPage";

const router = createBrowserRouter([
  {path:"/",element:<AppLayout/>,children:[
    {index:true,element:<DashboardPage/>},{path:"estoque",element:<EstoquePage/>},
    {path:"lotes",element:<LotesPage/>},{path:"lotes/:id",element:<LoteDetailPage/>},
    {path:"movimentacoes",element:<MovimentacoesPage/>},{path:"movimentacoes/nova",element:<NovaMovimentacaoPage/>},
    {path:"repositorio",element:<RepositorioPage/>},{path:"repositorio/nova",element:<PlantaFormPage/>},{path:"repositorio/:id",element:<PlantaDetailPage/>},{path:"repositorio/:id/editar",element:<PlantaFormPage/>},{path:"alertas",element:<AlertasPage/>},{path:"status",element:<StatusPage/>},
  ]},
]);

export function App() {
  return <EstoqueProvider><RouterProvider router={router} /></EstoqueProvider>;
}
