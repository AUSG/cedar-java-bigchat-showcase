// "7th Crew"는 "AUSG 7기 Crew" 워크스페이스의 페이지들을 읽을 수 있다
permit (
	principal in Notion::Role::"7th Crew",
	action in [
		Notion::Action::"VIEW_PAGE"
	],
	resource in Notion::Workspace::"AUSG 7기 Crew"
);

// "Manager"는 "AUSG 7기 운영진" 워크스페이스의 페이지들을 읽거나 수정할 수 있다
permit (
	principal in Notion::Role::"Manager",
	action in [
		Notion::Action::"VIEW_PAGE",
		Notion::Action::"EDIT_PAGE"
	],
	resource in Notion::Workspace::"AUSG 7기 운영진"
);

// "Manager"는 "AUSG 7기 Crew" 워크스페이스의 페이지들을 읽거나 수정할 수 있다
permit (
	principal in Notion::Role::"Manager",
	action in [
		Notion::Action::"VIEW_PAGE",
		Notion::Action::"EDIT_PAGE"
	],
	resource in Notion::Workspace::"AUSG 7기 Crew"
);

// "Admin"은 모든 페이지를 publish 할 수가 있다
permit (
	principal in Notion::Role::"Admin",
	action in [ 
		Notion::Action::"TOGGLE_PUBLISH_PAGE"
	],
	resource
);

// 모든 멤버는 각자 자신이 작성한 글을 편집할 수 있다
permit (
	principal,
	action in [
		Notion::Action::"EDIT_PAGE"
	],
	resource
)
when { resource has owner && resource.owner == principal};
