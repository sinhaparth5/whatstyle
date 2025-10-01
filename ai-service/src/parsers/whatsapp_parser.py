import re 
from dataclasses import dataclass
from typing import Iterator

@dataclass(slots=True)
class Message:
    timestamp: str
    sender: str
    content: str


class WhatsAppParser:
    __slots__ = ('_pattern',)
    
    def __init__(self) -> None:
        self._pattern = re.compile(
            r'\[?(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4},?\s+\d{1,2}:\d{2}(?::\d{2})?(?:\s*[AP]M)?)\]?\s*[-–]?\s*([^:]+):\s*(.+)',
            re.IGNORECASE
        )

    def parse(self, content: str) -> Iterator[Message]:
        for line in content.splitlines():
            if not line.strip():
                continue

            match = self._pattern.match(line)
            if not match:
                continue

            timestamp, sender, message = match.groups()
            yield Message(
                timestamp=timestamp.strip(),
                sender=sender.strip(),
                content=message.strip()
            )

    def parse_bytes(self, data: bytes) -> Iterator[Message]:
        try:
            content = data.decode('utf-8')
        except UnicodeDecodeError:
            content = data.decode('utf-8', errors='ignore')

        return self.parse(content)
