import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { HomePage } from './pages/HomePage'
import { LegalPage } from './pages/LegalPage'
import privacyMd from '../../PRIVACY.md?raw'
import termsCustomerMd from '../../TERMS_CUSTOMER.md?raw'
import termsVendorMd from '../../TERMS_VENDOR.md?raw'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route
          path="privatnost"
          element={
            <LegalPage
              title="Politika privatnosti"
              markdown={privacyMd}
            />
          }
        />
        <Route
          path="uslovi"
          element={
            <LegalPage
              title="Uslovi korišćenja — kupci"
              markdown={termsCustomerMd}
            />
          }
        />
        <Route
          path="uslovi-prodavci"
          element={
            <LegalPage
              title="Uslovi korišćenja — prodavci"
              markdown={termsVendorMd}
            />
          }
        />
        <Route path="privacy" element={<Navigate to="/privatnost" replace />} />
        <Route path="terms" element={<Navigate to="/uslovi" replace />} />
        <Route path="terms/vendor" element={<Navigate to="/uslovi-prodavci" replace />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
