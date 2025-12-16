const Unauthorized: React.FC = () => {
  return (
    <div className="container mt-5 text-center">
      <h1>🚫 Unauthorized</h1>
      <p>You do not have permission to view this page.</p>
      <a href="/login" className="btn btn-primary mt-3">Back to Login</a>
    </div>
  );
};

export default Unauthorized;
