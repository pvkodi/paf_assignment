import { createBrowserRouter } from 'react-router-dom'
import AppShell from '../app/AppShell'
import { DashboardPage, NotFoundPage, TicketsPage, TicketDetailPage, FacilitiesAndBookingsPage } from './pages'

const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: 'bookings',
        element: <FacilitiesAndBookingsPage />,
      },
      {
        path: 'tickets',
        element: <TicketsPage />,
      },
      {
        path: 'tickets/:id',
        element: <TicketDetailPage />,
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
])

export default router
