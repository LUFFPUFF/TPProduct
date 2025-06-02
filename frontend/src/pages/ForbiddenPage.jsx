import forbiddenImage from "../assets/okak.webp";

const ForbiddenPage = () => (
    <div className="p-10 text-center flex flex-col items-center">
        <h1 className="text-3xl font-bold text-red-600">403 - Доступ запрещён</h1>
        <p className="mt-4">У вас нет прав для доступа к этой странице.</p>
        <img
            src={forbiddenImage}
            alt="Доступ запрещён"
            className="w-70 h-70 mb-6"
        />
    </div>
);

export default ForbiddenPage;
