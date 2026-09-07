import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { HealthPage } from "@/pages/HealthPage";

const router = createBrowserRouter([
  {
    path: "/",
    element: <HealthPage />,
  },
]);

export function App() {
  return <RouterProvider router={router} />;
}
