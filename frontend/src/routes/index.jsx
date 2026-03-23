import { createBrowserRouter } from 'react-router-dom'
import AppShell from '../app/AppShell'
import { DashboardPage, NotFoundPage } from './pages'

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])

export default router
