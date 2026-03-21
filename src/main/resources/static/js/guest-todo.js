/**
 * guest-todo.js
 * 비로그인 사용자를 위한 LocalStorage 기반 Todo 관리
 */
var GuestTodo = (function () {
    var STORAGE_KEY = 'guest_todos';

    function isGuest() {
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

    /** "09:00" → 1시간 더한 "10:00" 반환 */
    function _addOneHour(timeStr) {
        var parts = timeStr.split(':');
        var h = parseInt(parts[0], 10) + 1;
        if (h > 23) h = 23;
        return h.toString().padStart(2, '0') + ':' + parts[1];
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
                startTime: todo.startTime || '',
                endTime: todo.endTime || '',
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
                // newDate가 "2025-03-21T10:00:00" 형태일 수 있음
                if (newDate && newDate.indexOf('T') >= 0) {
                    todo.deadline = newDate.split('T')[0];
                } else {
                    todo.deadline = newDate;
                }
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
                        description: t.description || '',
                        location: t.location || '',
                        completed: t.completed || false,
                        startTime: t.startTime || '',
                        endTime: t.endTime || ''
                    };

                    // ★★★ 핵심 수정: 시간/종일 분기 ★★★
                    if (t.startTime && t.startTime.length >= 4) {
                        // 시간 이벤트 — "2025-03-21T09:00" 형태
                        ev.start = t.deadline + 'T' + t.startTime;
                        ev.allDay = false;  // ★ 반드시 false

                        // ★ end를 반드시 설정 — 없으면 FullCalendar가 월말까지 이어지는 버그
                        if (t.endTime && t.endTime.length >= 4) {
                            ev.end = t.deadline + 'T' + t.endTime;
                        } else {
                            ev.end = t.deadline + 'T' + _addOneHour(t.startTime);
                        }
                    } else {
                        // 종일 이벤트 — 날짜만
                        ev.start = t.deadline;
                        ev.allDay = true;
                    }

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