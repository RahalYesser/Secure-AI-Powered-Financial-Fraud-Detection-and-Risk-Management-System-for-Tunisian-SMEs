import { useState, useEffect, ChangeEvent } from 'react';
import { userService } from '../services/userService';
import { UserResponse, UserRole, UserRegistrationRequest, UserUpdateRequest } from '../types/user.types';
import { PaginatedResponse } from '../types/common.types';
import { Modal } from '../components/ui/modal';
import Button from '../components/ui/button/Button';
import Badge from '../components/ui/badge/Badge';
import Input from '../components/form/input/InputField';
import Form from '../components/form/Form';
import { Table, TableHeader, TableBody, TableRow, TableCell } from '../components/ui/table';
import { usePermission } from '../hooks/usePermission';

const UserManagement = () => {
  const { hasPermission, user: currentUser } = usePermission();
  const [users, setUsers] = useState<PaginatedResponse<UserResponse> | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRole, setSelectedRole] = useState<UserRole | 'ALL'>('ALL');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Check permissions
  const canManage = hasPermission('USER_MANAGE');
  const canViewAll = hasPermission('USER_VIEW_ALL');

  useEffect(() => {
    if (canViewAll) {
      loadUsers();
    }
  }, [selectedRole]);

  const loadUsers = async (page = 0, size = 10) => {
    setLoading(true);
    setError(null);
    try {
      let response;
      if (selectedRole === 'ALL') {
        response = searchQuery
          ? await userService.searchUsers(searchQuery, { page, size })
          : await userService.getAllUsers({ page, size });
      } else {
        response = await userService.getUsersByRole(selectedRole as UserRole, { page, size });
      }
      setUsers(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    loadUsers();
  };

  const handleLockUnlock = async (userId: string, isLocked: boolean) => {
    try {
      if (isLocked) {
        await userService.unlockUser(userId);
      } else {
        await userService.lockUser(userId);
      }
      loadUsers();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleDelete = async (userId: string) => {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
      await userService.deleteUser(userId);
      loadUsers();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete user');
    }
  };

  const handleEdit = (user: UserResponse) => {
    setSelectedUser(user);
    setIsEditModalOpen(true);
  };

  const getRoleBadgeColor = (role: UserRole) => {
    switch (role) {
      case UserRole.ADMIN:
        return 'error';
      case UserRole.FINANCIAL_ANALYST:
        return 'info';
      case UserRole.AUDITOR:
        return 'warning';
      case UserRole.SME_USER:
        return 'success';
      default:
        return 'light';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (!canViewAll) {
    return (
      <div className="p-6">
        <div className="rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          You don't have permission to view users.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">User Management</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Manage system users, roles, and permissions
          </p>
        </div>
        {canManage && (
          <Button onClick={() => setIsCreateModalOpen(true)}>
            Create User
          </Button>
        )}
      </div>

      {/* Filters */}
      <div className="mb-6 flex gap-4">
        <div className="flex-1">
          <Input
            type="text"
            placeholder="Search by name or email..."
            value={searchQuery}
            onChange={(e: ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="w-48">
          <select
            className="h-11 w-full appearance-none rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm"
            value={selectedRole}
            onChange={(e: ChangeEvent<HTMLSelectElement>) => setSelectedRole(e.target.value as UserRole | 'ALL')}
          >
            <option value="ALL">All Roles</option>
            <option value={UserRole.ADMIN}>Admin</option>
            <option value={UserRole.FINANCIAL_ANALYST}>Financial Analyst</option>
            <option value={UserRole.AUDITOR}>Auditor</option>
            <option value={UserRole.SME_USER}>SME User</option>
          </select>
        </div>
        <Button onClick={handleSearch}>Search</Button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      {/* Users Table */}
      <div className="rounded-lg border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-900">
        <Table>
          <TableHeader>
            <TableRow>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                User
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Role
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Last Login
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Created
              </th>
              {canManage && (
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Actions
                </th>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8">
                  Loading...
                </TableCell>
              </TableRow>
            ) : users?.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8 text-gray-500">
                  No users found
                </TableCell>
              </TableRow>
            ) : (
              users?.content.map((user: UserResponse) => (
                <TableRow key={user.id}>
                  <TableCell className="px-6 py-4">
                    <div>
                      <div className="font-medium text-gray-900 dark:text-white">
                        {user.firstName} {user.lastName}
                      </div>
                      <div className="text-sm text-gray-500 dark:text-gray-400">
                        {user.email}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    <Badge color={getRoleBadgeColor(user.role)}>
                      {user.role.replace('_', ' ')}
                    </Badge>
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    {user.accountLocked ? (
                      <Badge color="error">Locked</Badge>
                    ) : (
                      <Badge color="success">Active</Badge>
                    )}
                  </TableCell>
                  <TableCell className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                    {user.lastLoginAt ? formatDate(user.lastLoginAt) : 'Never'}
                  </TableCell>
                  <TableCell className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                    {formatDate(user.createdAt)}
                  </TableCell>
                  {canManage && (
                    <TableCell className="px-6 py-4 text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleEdit(user)}
                        >
                          Edit
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleLockUnlock(user.id, user.accountLocked)}
                        >
                          {user.accountLocked ? 'Unlock' : 'Lock'}
                        </Button>
                        {user.id !== currentUser?.id && (
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleDelete(user.id)}
                          >
                            Delete
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {users && users.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <div className="text-sm text-gray-500 dark:text-gray-400">
            Showing {users.content.length} of {users.totalElements} users
          </div>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={users.number === 0}
              onClick={() => loadUsers(users.number - 1)}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={users.number >= users.totalPages - 1}
              onClick={() => loadUsers(users.number + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Create User Modal */}
      <CreateUserModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          setIsCreateModalOpen(false);
          loadUsers();
        }}
      />

      {/* Edit User Modal */}
      {selectedUser && (
        <EditUserModal
          isOpen={isEditModalOpen}
          user={selectedUser}
          onClose={() => {
            setIsEditModalOpen(false);
            setSelectedUser(null);
          }}
          onSuccess={() => {
            setIsEditModalOpen(false);
            setSelectedUser(null);
            loadUsers();
          }}
        />
      )}
    </div>
  );
};

// Create User Modal Component
const CreateUserModal = ({ isOpen, onClose, onSuccess }: any) => {
  const [formData, setFormData] = useState<UserRegistrationRequest>({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    role: UserRole.SME_USER,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await userService.registerUser(formData);
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create user');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} className="max-w-md p-6">
      <h2 className="mb-4 text-xl font-semibold text-gray-900 dark:text-white">Create User</h2>
      
      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-3 text-sm text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      <Form onSubmit={handleSubmit}>
        <div className="space-y-4">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Email
            </label>
            <Input
              type="email"
              value={formData.email}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, email: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Password
            </label>
            <Input
              type="password"
              value={formData.password}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, password: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              First Name
            </label>
            <Input
              type="text"
              value={formData.firstName}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, firstName: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Last Name
            </label>
            <Input
              type="text"
              value={formData.lastName}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, lastName: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Role
            </label>
            <select
              className="h-11 w-full appearance-none rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm"
              value={formData.role}
              onChange={(e: ChangeEvent<HTMLSelectElement>) => setFormData({ ...formData, role: e.target.value as UserRole })}
            >
              <option value={UserRole.ADMIN}>Admin</option>
              <option value={UserRole.FINANCIAL_ANALYST}>Financial Analyst</option>
              <option value={UserRole.AUDITOR}>Auditor</option>
              <option value={UserRole.SME_USER}>SME User</option>
            </select>
          </div>

          <div className="flex gap-3 pt-4">
            <Button disabled={loading} className="flex-1">
              {loading ? 'Creating...' : 'Create User'}
            </Button>
            <Button variant="outline" onClick={onClose} className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

// Edit User Modal Component
const EditUserModal = ({ isOpen, user, onClose, onSuccess }: any) => {
  const [formData, setFormData] = useState<UserUpdateRequest>({
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    role: user.role,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await userService.updateUser(user.id, formData);
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update user');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} className="max-w-md p-6">
      <h2 className="mb-4 text-xl font-semibold text-gray-900 dark:text-white">Edit User</h2>
      
      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-3 text-sm text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      <Form onSubmit={handleSubmit}>
        <div className="space-y-4">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Email
            </label>
            <Input
              type="email"
              value={formData.email}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, email: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              First Name
            </label>
            <Input
              type="text"
              value={formData.firstName}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, firstName: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Last Name
            </label>
            <Input
              type="text"
              value={formData.lastName}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, lastName: e.target.value })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Role
            </label>
            <select
              className="h-11 w-full appearance-none rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm"
              value={formData.role}
              onChange={(e: ChangeEvent<HTMLSelectElement>) => setFormData({ ...formData, role: e.target.value as UserRole })}
            >
              <option value={UserRole.ADMIN}>Admin</option>
              <option value={UserRole.FINANCIAL_ANALYST}>Financial Analyst</option>
              <option value={UserRole.AUDITOR}>Auditor</option>
              <option value={UserRole.SME_USER}>SME User</option>
            </select>
          </div>

          <div className="flex gap-3 pt-4">
            <Button disabled={loading} className="flex-1">
              {loading ? 'Updating...' : 'Update User'}
            </Button>
            <Button variant="outline" onClick={onClose} className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default UserManagement;
