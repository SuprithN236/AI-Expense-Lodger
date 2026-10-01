import { AuthScreen } from './components/AuthScreen';
import { Dashboard } from './components/Dashboard';
import { useAuth } from './context/AuthContext';

export default function App() {
  const { user } = useAuth();
  return user ? <Dashboard /> : <AuthScreen />;
}
