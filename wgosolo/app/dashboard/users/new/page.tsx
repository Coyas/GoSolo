import UserForm from "../../../components/UserForm";

export default function NewUserPage() {
	return (
		<div>
			<h1 className="text-xl font-semibold text-zinc-900 mb-6">
				Novo colaborador
			</h1>
			<UserForm />
		</div>
	);
}
