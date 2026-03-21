/**
 * guest-todo.js
 * 비로그인 사용자를 위한 LocalStorage 기반 Todo 관리
 */
var GuestTodo = (function () {
    var STORAGE_KEY = 'guest_todos';

    function isGuest() {
        // window.__loggedIn이 정확히 true인 경우만 로그인 상태
        return window.__loggedIn !== true;
    }

    function _load() {
        try {
            var raw = localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }

    function _save(todos) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(todos));
    }

    function _nextId() {
        var todos = _load();
        if (todos.length === 0) return 1;
        return Math.max.apply(null, todos.map(function(t){ return t.id; })) + 1;
    }

    function _today() {
        return new Date().toISOString().split('T')[0];
    }

    function _startOfWeek() {
        var d = new Date();
        var day = d.getDay();
        var diff = d.getDate() - day + (day === 0 ? -6 : 1);
        var mon = new Date(d);
        mon.setDate(diff);
        return mon.toISOString().split('T')[0];
    }

    function _endOfWeek() {
        var d = new Date();
        var day = d.getDay();
        var diff = d.getDate() - day + (day === 0 ? 0 : 7);
        var sun = new Date(d);
        sun.setDate(diff);
        return sun.toISOString().split('T')[0];
    }

    return {
        isGuest: isGuest,

        getAll: function () {
            return _load().sort(function(a, b) { return (a.sortOrder || 0) - (b.sortOrder || 0); });
        },

        add: function (todo) {
            var todos = _load();
            var newTodo = {
                id: _nextId(),
                title: todo.title || '',
                description: todo.description || '',
                deadline: todo.deadline || null,
                priority: todo.priority || 'LOW',
                color: todo.color || '',
                repeatType: todo.repeatType || 'NONE',
                project: todo.project || '',
                location: todo.location || '',
                completed: false,
                sortOrder: todos.length,
                createdAt: new Date().toISOString()
            };
            todos.push(newTodo);
            _save(todos);
            return newTodo;
        },

        toggle: function (id) {
            var todos = _load();
            var todo = todos.find(function(t){ return t.id === id; });
            if (todo) {
                todo.completed = !todo.completed;
                _save(todos);
            }
        },

        remove: function (id) {
            var todos = _load().filter(function(t){ return t.id !== id; });
            _save(todos);
        },

        removeRepeat: function (title, repeatType) {
            var todos = _load().filter(function(t){ return !(t.title === title && t.repeatType === repeatType); });
            _save(todos);
        },

        reorder: function (ids) {
            var todos = _load();
            ids.forEach(function(id, index) {
                var todo = todos.find(function(t){ return t.id === parseInt(id); });
                if (todo) todo.sortOrder = index;
            });
            _save(todos);
        },

        updateDate: function (id, newDate) {
            var todos = _load();
            var todo = todos.find(function(t){ return t.id === parseInt(id); });
            if (todo) {
                todo.deadline = newDate;
                _save(todos);
            }
        },

        today: function () {
            var td = _today();
            return this.getAll().filter(function(t){ return t.deadline === td; });
        },

        week: function () {
            var start = _startOfWeek();
            var end = _endOfWeek();
            return this.getAll().filter(function(t){ return t.deadline && t.deadline >= start && t.deadline <= end; });
        },

        overdue: function () {
            var td = _today();
            return this.getAll().filter(function(t){ return !t.completed && t.deadline && t.deadline < td; });
        },

        urgent: function (limit) {
            return this.getAll()
                .filter(function(t){ return !t.completed && t.deadline; })
                .sort(function(a, b){ return a.deadline.localeCompare(b.deadline); })
                .slice(0, limit || 5);
        },

        search: function (q) {
            if (!q) return this.getAll();
            var lower = q.toLowerCase();
            return this.getAll().filter(function(t){
                return (t.title && t.title.toLowerCase().indexOf(lower) >= 0) ||
                    (t.description && t.description.toLowerCase().indexOf(lower) >= 0) ||
                    (t.project && t.project.toLowerCase().indexOf(lower) >= 0);
            });
        },

        toCalendarEvents: function () {
            return this.getAll()
                .filter(function(t){ return t.deadline; })
                .map(function(t) {
                    var prefix = t.priority === 'HIGH' ? '🔥 ' : t.priority === 'MEDIUM' ? '⚡ ' : '🌿 ';
                    var ev = {
                        id: String(t.id),
                        title: prefix + t.title,
                        start: t.deadline,
                        description: t.description || ''
                    };
                    if (t.completed) ev.classNames = ['completed-event'];
                    if (t.priority === 'HIGH') ev.color = '#dc3545';
                    else if (t.priority === 'MEDIUM') ev.color = '#ffc107';
                    else ev.color = '#198754';
                    return ev;
                });
        },

        stats: function () {
            var all = this.getAll();
            var completed = all.filter(function(t){ return t.completed; });
            return { total: all.length, completed: completed.length, incomplete: all.length - completed.length };
        }
    };
})();