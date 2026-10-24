package com.gradtrack.app.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.gradtrack.app.R
import com.gradtrack.app.data.model.Role
import com.gradtrack.app.data.repository.AuthRepository
import com.gradtrack.app.databinding.FragmentLoginBinding
import com.gradtrack.app.databinding.ViewRoleOptionBinding
import com.gradtrack.app.ui.common.BaseViewModel
import com.gradtrack.app.ui.common.GradFragment
import com.gradtrack.app.ui.common.UiEvent
import com.gradtrack.app.ui.common.appViewModels
import com.gradtrack.app.ui.common.collect
import com.gradtrack.app.ui.common.container
import com.gradtrack.app.ui.common.hideKeyboard
import com.gradtrack.app.ui.common.observeEvents
import com.gradtrack.app.ui.common.showMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel(private val auth: AuthRepository) : BaseViewModel() {
    private val _role = MutableStateFlow(Role.STUDENT)

    /** Which login is showing. Students are the default; leaders and supervisors switch with the buttons below. */
    val role: StateFlow<Role> = _role.asStateFlow()

    fun select(role: Role) {
        _role.value = role
    }

    fun login(identifier: String, password: String) = act({ auth.login(identifier, password, role.value) }) { send(UiEvent.Done()) }
}

/** After sign-in: the role's home, with login removed from the back stack. */
fun GradFragment.goHome() {
    val home = if (container.session.user?.role == Role.SUPERVISOR) R.id.supervisorDashboardFragment else R.id.studentDashboardFragment
    findNavController().navigate(home, null, navOptions { popUpTo(R.id.nav_graph) { inclusive = true } })
}

class LoginFragment : GradFragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel by appViewModels { LoginViewModel(it.auth) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener { submit() }
        binding.password.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) submit()
            actionId == EditorInfo.IME_ACTION_DONE
        }
        binding.forgotPassword.setOnClickListener { showMessage(getString(R.string.forgot_password_soon)) }

        collect(viewModel.role) { renderRole(it) }
        collect(viewModel.busy) { busy ->
            binding.loginButton.isEnabled = !busy
            binding.roleOptionA.root.isEnabled = !busy
            binding.roleOptionB.root.isEnabled = !busy
            binding.loginProgress.visibility = if (busy) View.VISIBLE else View.INVISIBLE
        }
        observeEvents(viewModel) { goHome() }
    }

    private fun renderRole(role: Role) {
        binding.loginTitle.setText(
            when (role) {
                Role.STUDENT -> R.string.login_title_student
                Role.LEADER -> R.string.login_title_leader
                Role.SUPERVISOR -> R.string.login_title_supervisor
            },
        )
        binding.loginHelper.setText(
            when (role) {
                Role.STUDENT -> R.string.login_helper_student
                Role.LEADER -> R.string.login_helper_leader
                Role.SUPERVISOR -> R.string.login_helper_supervisor
            },
        )
        binding.identifierLayout.error = null
        binding.passwordLayout.error = null
        binding.loginButton.setText(
            when (role) {
                Role.STUDENT -> R.string.login_as_student
                Role.LEADER -> R.string.login_as_leader
                Role.SUPERVISOR -> R.string.login_as_supervisor
            },
        )
        // The two buttons always offer the other two logins, so a leader can get back to the student login too.
        binding.continueHint.setText(if (role == Role.STUDENT) R.string.or_continue_as_hint else R.string.or_continue_as_switch)
        val others = Role.entries.filter { it != role }
        bindOption(binding.roleOptionA, others[0])
        bindOption(binding.roleOptionB, others[1])
    }

    private fun bindOption(option: ViewRoleOptionBinding, role: Role) {
        val (icon, title) =
            when (role) {
                Role.STUDENT -> R.drawable.ic_school to R.string.role_student
                Role.LEADER -> R.drawable.ic_team to R.string.role_team_leader
                Role.SUPERVISOR -> R.drawable.ic_proposals to R.string.role_supervisor
            }
        option.roleIcon.setImageResource(icon)
        option.roleTitle.setText(title)
        option.root.contentDescription = getString(R.string.login_as_role, getString(title))
        option.root.setOnClickListener {
            binding.identifier.text = null
            binding.password.text = null
            viewModel.select(role)
            binding.root.smoothScrollTo(0, 0)
        }
    }

    private fun submit() {
        val identifier = binding.identifier.text?.toString().orEmpty().trim()
        val password = binding.password.text?.toString().orEmpty()
        binding.identifierLayout.error = if (identifier.isEmpty()) getString(R.string.error_required) else null
        binding.passwordLayout.error = if (password.isEmpty()) getString(R.string.error_required) else null
        if (identifier.isEmpty() || password.isEmpty()) return
        hideKeyboard()
        viewModel.login(identifier, password)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
